package com.monokek.ordering.domain;

import com.monokek.common.ApiException;
import com.monokek.common.Timestamps;
import com.monokek.ordering.domain.event.OrderCreatedEvent;
import com.monokek.ordering.domain.event.OrderStatusChangedEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root: port of {@code App\Models\Order}, owning its rounds
 * (which own their items, which own their modifiers) and its status
 * history — the same composition Laravel expresses through
 * {@code hasMany}/{@code hasManyThrough} relations, here enforced by JPA
 * cascades instead of being assembled ad hoc by every caller.
 *
 * <p>Kitchen ticket creation is deliberately <em>not</em> a method here: it
 * needs {@code catalog.ProductCatalog} to resolve a product's kitchen
 * station, and an aggregate must never reach into another module. That
 * resolution — and the resulting {@code KitchenTicketRequestedEvent} — is
 * the application service's job (see {@code ordering.application.OrderService}).
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private UUID uuid;

    private String reference;

    /** References company.Branch by id only — see module package-info. */
    @Column(name = "branch_id")
    private Long branchId;

    /** References floorplan.RestaurantTable by id only. */
    @Column(name = "table_id")
    private Long tableId;

    /** References crm.Customer by id only. */
    @Column(name = "customer_id")
    private Long customerId;

    /** References identity.User by id only — the waiter who opened the order. */
    @Column(name = "user_id")
    private Long userId;

    /** References identity.User by id only — who will/did cash it out. */
    @Column(name = "cashier_id")
    private Long cashierId;

    private String type = "dinein"; // dinein, takeaway, delivery

    private String status = "draft";

    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    private String source;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderRound> rounds = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderStatusHistory> statusHistories = new ArrayList<>();

    @Transient
    private final transient List<UnresolvedEvent> pendingEvents = new ArrayList<>();

    @PrePersist
    void assignUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }

    /** Opens a brand-new order for a table. Stages an {@link OrderCreatedEvent}. */
    public static Order openForTable(Long branchId, Long tableId, Long waiterUserId, Long cashierUserId) {
        Order order = new Order();
        order.branchId = branchId;
        order.tableId = tableId;
        order.userId = waiterUserId;
        order.cashierId = cashierUserId;
        order.status = "pending";
        order.type = "dinein";
        order.reference = "CMD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        order.pendingEvents.add(new UnresolvedEvent(EventKind.CREATED, null));
        return order;
    }

    /** Opens a new manager-booked reservation order (no table assigned yet, status "reserved"). */
    public static Order openReservation(Long branchId, Long customerId, Long userId) {
        Order order = new Order();
        order.branchId = branchId;
        order.customerId = customerId;
        order.userId = userId;
        order.cashierId = userId;
        order.status = "reserved";
        order.source = "manager_booking";
        order.reference = "CMD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        order.pendingEvents.add(new UnresolvedEvent(EventKind.CREATED, null));
        return order;
    }

    /**
     * Opens the next round (round_number auto-incremented) and attaches it to this order.
     *
     * <p>Reopens the order first if it had already reached {@code completed}: that status is set
     * by {@link #completeIfAllRoundsResolved} once every existing round is served/voided, but nothing
     * previously undid it when a further round was added afterwards (a guest orders another drink
     * before the bill). Left alone, the order stayed "completed" — billable in the POS — for the
     * entire time this new round was still being prepared, and once THAT round finished,
     * {@code completeIfAllRoundsResolved}'s own {@code !"completed".equals(status)} guard silently
     * swallowed the follow-up transition (no new {@link OrderStatusChangedEvent}, so
     * {@code KitchenTicketListener}/{@code ActivityLogListener}/{@code NotificationEventListener}
     * never reacted to it either).
     */
    public OrderRound openRound(String note) {
        if ("completed".equals(status)) {
            changeStatus("pending", null);
        }
        int nextNumber = rounds.size() + 1;
        OrderRound round = OrderRound.open(this, nextNumber, note);
        rounds.add(round);
        return round;
    }

    /** Recomputes subtotal/total from every item across every round — port of {@code Order::refreshTotals}. */
    public void refreshTotals() {
        BigDecimal newSubtotal = allItems().stream().map(OrderItem::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.subtotal = newSubtotal;
        this.total = newSubtotal.add(tax).subtract(discount);
    }

    /** Excludes voided rounds — a voided round's items are removed from the bill, not just hidden. */
    public List<OrderItem> allItems() {
        return rounds.stream().filter(r -> !"voided".equals(r.getStatus())).flatMap(r -> r.getItems().stream()).toList();
    }

    /** Transitions status, appends an {@link OrderStatusHistory} entry and stages an {@link OrderStatusChangedEvent}. */
    public void changeStatus(String newStatus, Long changedByUserId) {
        changeStatus(newStatus, changedByUserId, null);
    }

    /** Same as {@link #changeStatus(String, Long)}, with a free-text reason recorded on the history entry — see {@link #cancel}. */
    public void changeStatus(String newStatus, Long changedByUserId, String reason) {
        String previous = this.status;
        this.status = newStatus;
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(this);
        history.setStatus(newStatus);
        history.setUserId(changedByUserId);
        history.setReason(reason);
        statusHistories.add(history);
        pendingEvents.add(new UnresolvedEvent(EventKind.STATUS_CHANGED, new Object[]{previous, newStatus, changedByUserId, reason}));
    }

    public void markPaid(Long cashierUserId) {
        this.paidAt = LocalDateTime.now();
        changeStatus("paid", cashierUserId);
    }

    public boolean isPaid() {
        return "paid".equals(status);
    }

    public boolean isCancelled() {
        return "cancelled".equals(status);
    }

    /**
     * Voids an order that never got paid — e.g. the guest left without settling the bill. A paid
     * order needs a refund flow, not a cancellation (billing is already reconciled); an
     * already-cancelled one can't be cancelled twice. The reason is mandatory at the service layer
     * ({@code OrderService#cancelOrder}), not re-validated here — this method just refuses to run
     * on a status where "why" wouldn't make sense.
     */
    public void cancel(Long cancelledByUserId, String reason) {
        if (isPaid()) {
            throw ApiException.conflict("Impossible d'annuler une commande déjà payée.");
        }
        if (isCancelled()) {
            throw ApiException.conflict("Cette commande est déjà annulée.");
        }
        changeStatus("cancelled", cancelledByUserId, reason);
    }

    /** Port of {@code TableController::transfer}'s {@code $order->update(['table_id' => ...])}. */
    public void transferToTable(Long newTableId) {
        this.tableId = newTableId;
    }

    /**
     * Reacts to the kitchen's own aggregation of its tickets for one round —
     * called synchronously from {@code kitchen.application.KitchenTicketService}
     * through the {@code ordering.OrderRoundStatusUpdater} published interface
     * (kitchen already depends on ordering for ticket creation, so this keeps
     * the dependency one-directional instead of ordering listening back to a
     * kitchen event, which would create a module cycle). Port of the
     * round/order cascade Laravel recomputes inline in
     * {@code TicketController::updateStatus}/{@code updateStatusDirect}.
     * Both Laravel methods existed with slightly different final order
     * statuses ({@code ready} vs {@code completed}) once every round is
     * served; this consolidates on {@code completed}, the newer/more
     * complete of the two.
     */
    public void applyKitchenRoundStatus(Long roundId, String newRoundStatus, Long systemUserId) {
        OrderRound round = rounds.stream().filter(r -> r.getId().equals(roundId)).findFirst().orElse(null);
        if (round == null || newRoundStatus.equals(round.getStatus())) {
            return;
        }
        round.markKitchenStatus(newRoundStatus);
        completeIfAllRoundsResolved(systemUserId);
    }

    /**
     * Same "every round done" cascade {@link #applyKitchenRoundStatus} runs, exposed for callers
     * that change a round's status through a different path — {@code OrderService#voidRound}
     * voids a round directly (there's no kitchen ticket to update if nothing was ever sent),
     * which needs this same completion check without going through a kitchen ticket status update.
     */
    public void completeIfAllRoundsResolved(Long systemUserId) {
        if (allRoundsResolved() && !"completed".equals(status) && !isPaid()) {
            changeStatus("completed", systemUserId);
        }
    }

    /** A round counts as resolved once nothing more will ever happen to it: served, or voided off the bill entirely. */
    private boolean allRoundsResolved() {
        return !rounds.isEmpty() && rounds.stream().allMatch(r -> "served".equals(r.getStatus()) || "voided".equals(r.getStatus()));
    }

    @DomainEvents
    Collection<Object> domainEvents() {
        return pendingEvents.stream().map(this::resolve).toList();
    }

    @AfterDomainEventPublication
    void clearDomainEvents() {
        pendingEvents.clear();
    }

    private Object resolve(UnresolvedEvent pending) {
        return switch (pending.kind) {
            case CREATED -> new OrderCreatedEvent(id, uuid, reference, branchId, tableId, userId);
            case STATUS_CHANGED -> {
                Object[] payload = (Object[]) pending.payload;
                List<OrderStatusChangedEvent.SoldItem> soldItems = allItems().stream()
                        .map(item -> new OrderStatusChangedEvent.SoldItem(item.getProductId(), item.getQty()))
                        .toList();
                yield new OrderStatusChangedEvent(
                        id, uuid, branchId, (String) payload[0], (String) payload[1], (Long) payload[2], (String) payload[3], soldItems);
            }
        };
    }

    private enum EventKind { CREATED, STATUS_CHANGED }

    private record UnresolvedEvent(EventKind kind, Object payload) {
    }

    /**
     * Guards every write that appends to an order (new round, round edit, resuming an order
     * an explicit {@code orderId} points at). Checks both terminal states, not just paid: a
     * cancelled order used to only be excluded from the table's "current open order" lookup by
     * its status matching "paid" ({@code findFirstByTableIdAndStatusNotInOrderByIdDesc} used to
     * take a single excluded status), so a cancelled-but-not-paid order on a real (non-virtual)
     * table was silently picked back up as "the" order the next time a round was sent to that
     * table — new rounds/items landed on it, it re-occupied the table via
     * {@code tableDirectory.markOccupied}, kitchen tickets fired normally (independent of the
     * order's status), but the order itself stayed permanently invisible in the POS ("Ventes")
     * history: {@code OrderService#history}'s active-orders query explicitly excludes
     * "cancelled". The table looked stuck "occupied" with an order nobody could ever see or bill.
     */
    public void assertOpen() {
        if (isPaid()) {
            throw ApiException.conflict("Impossible d'ajouter un round : la commande est déjà payée.");
        }
        if (isCancelled()) {
            throw ApiException.conflict("Impossible d'ajouter un round : la commande est annulée.");
        }
    }
}
