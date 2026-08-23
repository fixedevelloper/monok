package com.monokek.ordering.domain;

import com.monokek.common.ApiException;
import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_rounds")
@Getter
@NoArgsConstructor
public class OrderRound extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "round_number")
    private int roundNumber = 1;

    /** pending, sent, preparing, served, voided */
    private String status = "pending";

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /** Manager who approved the void via PIN (see identity.ManagerAuthClient) — null unless status is "voided". */
    @Column(name = "voided_by_user_id")
    private Long voidedByUserId;

    @Column(name = "void_reason", columnDefinition = "TEXT")
    private String voidReason;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @OneToMany(mappedBy = "orderRound", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    static OrderRound open(Order order, int roundNumber, String note) {
        OrderRound round = new OrderRound();
        round.order = order;
        round.roundNumber = roundNumber;
        round.status = "sent";
        round.note = note;
        round.sentAt = LocalDateTime.now();
        return round;
    }

    /** Adds a priced item — price is always resolved by the caller from the catalog, never trusted from the client. */
    public OrderItem addItem(Long productId, Long variantId, int qty, BigDecimal unitPrice) {
        OrderItem item = OrderItem.of(this, productId, variantId, qty, unitPrice);
        items.add(item);
        return item;
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
    }

    /**
     * A round can only be edited (qty changed, item added) while it's still
     * {@code sent} and its order isn't {@code paid} — port of
     * {@code OrderController::assertRoundIsEditable}.
     */
    public void assertEditable() {
        if (!"sent".equals(status)) {
            throw ApiException.conflict("Ce round ne peut plus être modifié (déjà servi ou annulé).");
        }
        if (order.isPaid()) {
            throw ApiException.conflict("Impossible de modifier : la commande est déjà payée.");
        }
    }

    public BigDecimal totalRound() {
        return items.stream().map(OrderItem::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Applied by {@code Order#applyKitchenRoundStatus} in reaction to kitchen ticket updates. */
    void markKitchenStatus(String status) {
        this.status = status;
    }

    /**
     * Removes this round from the bill without physically deleting it — called by
     * {@code OrderService#voidRound} only after a manager's PIN was verified. Refuses on an
     * already-paid order (billing is reconciled — use a refund flow instead) and on a round
     * that's already {@code served} or {@code voided}. Any kitchen ticket already created for
     * this round is cancelled separately, in reaction to {@code RoundVoidedEvent} — voiding
     * doesn't un-send an order that's already in the kitchen, it only stops it from being billed.
     */
    public void voidRound(Long approvingManagerUserId, String reason) {
        if (order.isPaid()) {
            throw ApiException.conflict("Impossible de supprimer un round : la commande est déjà payée.");
        }
        if ("served".equals(status) || "voided".equals(status)) {
            throw ApiException.conflict("Ce round ne peut plus être supprimé (déjà servi ou déjà supprimé).");
        }
        this.status = "voided";
        this.voidedByUserId = approvingManagerUserId;
        this.voidReason = reason;
        this.voidedAt = LocalDateTime.now();
    }
}
