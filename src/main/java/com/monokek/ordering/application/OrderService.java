package com.monokek.ordering.application;

import com.monokek.cashier.CashierFacade;
import com.monokek.catalog.ProductCatalog;
import com.monokek.common.ApiException;
import com.monokek.floorplan.TableDirectory;
import com.monokek.identity.UserDirectory;
import com.monokek.pms.PmsClient;
import com.monokek.ordering.domain.*;
import com.monokek.ordering.domain.event.KitchenTicketRequestedEvent;
import com.monokek.ordering.domain.event.OrderPaidEvent;
import com.monokek.ordering.web.dto.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

/**
 * Application service: port of {@code App\Http\Controllers\Api\Pos\OrderController}.
 * Cross-module reads/writes (product pricing, table occupancy, cash session,
 * user names) go through the published interfaces of {@code catalog},
 * {@code floorplan}, {@code cashier} and {@code identity} — never through a
 * direct entity reference.
 *
 * <p>Simplifications versus the Laravel version, called out explicitly
 * rather than silently dropped: no pessimistic row locking on
 * table/order/round (Laravel's {@code lockForUpdate()}); {@code index}/
 * {@code historyAdmin} search matches on {@code reference} only, not table
 * name or waiter name (those live in other modules and would need a join
 * across the module boundary); {@code history} merges "paid in this
 * session" + "active today" in memory and caps at 50 rather than true
 * cursor pagination, mirroring the two conditions Laravel OR's together.
 */
@Service
public class OrderService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<String> ACTIVE_ORDER_STATUSES =
            List.of("pending", "preparing", "ready", "billing", "pending_payment", "completed");

    private final OrderRepository orderRepository;
    private final OrderRoundRepository orderRoundRepository;
    private final ProductCatalog productCatalog;
    private final TableDirectory tableDirectory;
    private final CashierFacade cashierFacade;
    private final UserDirectory userDirectory;
    private final CommissionService commissionService;
    private final ApplicationEventPublisher events;
    private final PmsClient pmsClient;

    public OrderService(
            OrderRepository orderRepository,
            OrderRoundRepository orderRoundRepository,
            ProductCatalog productCatalog,
            TableDirectory tableDirectory,
            CashierFacade cashierFacade,
            UserDirectory userDirectory,
            CommissionService commissionService,
            ApplicationEventPublisher events,
            PmsClient pmsClient) {
        this.orderRepository = orderRepository;
        this.orderRoundRepository = orderRoundRepository;
        this.productCatalog = productCatalog;
        this.tableDirectory = tableDirectory;
        this.cashierFacade = cashierFacade;
        this.userDirectory = userDirectory;
        this.commissionService = commissionService;
        this.events = events;
        this.pmsClient = pmsClient;
    }

    @Transactional
    public SendRoundResult sendRound(SendRoundRequest request, Long waiterUserId, Long cashierUserId) {
        TableDirectory.TableSnapshot table = tableDirectory.findTable(request.tableId())
                .orElseThrow(() -> ApiException.notFound("Table introuvable."));

        Order order = resolveOrderForSendRound(request, table, waiterUserId, cashierUserId);
        OrderRound round = order.openRound(request.note());

        Map<Long, List<KitchenTicketRequestedEvent.TicketItem>> itemsByStation = new LinkedHashMap<>();
        List<SendRoundResult.SkippedItem> skipped = new ArrayList<>();

        for (SendRoundRequest.ItemLine line : request.items()) {
            ProductCatalog.ProductSnapshot product = productCatalog.findProduct(line.productId())
                    .orElseThrow(() -> ApiException.badRequest("Produit introuvable : " + line.productId()));

            OrderItem item = round.addItem(product.id(), null, line.qty(), product.price());
            addModifiers(item, line.modifiers());

            if (product.kitchenStationId() != null) {
                itemsByStation.computeIfAbsent(product.kitchenStationId(), id -> new ArrayList<>())
                        .add(new KitchenTicketRequestedEvent.TicketItem(product.name(), line.qty(), modifierNames(line.modifiers())));
            } else {
                skipped.add(new SendRoundResult.SkippedItem(
                        null, product.id(), product.name(), "Aucune station de cuisine définie pour la catégorie de ce produit."));
            }
        }

        order.refreshTotals();
        order = orderRepository.save(order);
        // Explicit save, not just relying on order.rounds' cascade: when `order` was fetched via
        // findById (every round after the first one on an already-open order), it's already
        // managed, so orderRepository.save(order) above is a merge() no-op that never triggers an
        // insert for the newly-added `round` — Hibernate would only flush it (and assign the
        // IDENTITY-generated id) at transaction commit, which is too late for round.getId() below.
        // Saving `round` directly forces that insert now. (A brand-new order doesn't have this
        // problem — its save() is a real persist(), which cascades immediately — but this call is
        // a harmless no-op in that case.)
        round = orderRoundRepository.save(round);
        final Long orderId = order.getId();
        final Long roundId = round.getId();

        if (!table.virtual()) {
            tableDirectory.markOccupied(table.id());
        }
        String serverName = userNameOrNull(order.getUserId());
        itemsByStation.forEach((stationId, items) -> events.publishEvent(
                new KitchenTicketRequestedEvent(orderId, roundId, table.branchId(), stationId, null, table.name(), items, serverName)));

        return new SendRoundResult(toDto(order), skipped);
    }

    private Order resolveOrderForSendRound(
            SendRoundRequest request, TableDirectory.TableSnapshot table, Long waiterUserId, Long cashierUserId) {
        if (request.orderId() != null) {
            Order order = orderRepository.findById(request.orderId())
                    .orElseThrow(() -> ApiException.notFound("Commande introuvable."));
            order.assertNotPaid();
            if (!Objects.equals(order.getTableId(), table.id())) {
                throw ApiException.conflict("Cette commande n'appartient pas à la table indiquée.");
            }
            return order;
        }
        // A virtual table represents no one in particular — each visit is a different, independent
        // customer, so reusing whatever order was last opened for it would silently merge two
        // strangers' bills together. Always start fresh instead.
        if (table.virtual()) {
            return Order.openForTable(table.branchId(), table.id(), waiterUserId, cashierUserId);
        }
        return orderRepository.findFirstByTableIdAndStatusNotOrderByIdDesc(table.id(), "paid")
                .orElseGet(() -> Order.openForTable(table.branchId(), table.id(), waiterUserId, cashierUserId));
    }

    private void addModifiers(OrderItem item, List<SendRoundRequest.ModifierLine> modifiers) {
        if (modifiers == null) {
            return;
        }
        for (SendRoundRequest.ModifierLine mod : modifiers) {
            ProductCatalog.ModifierItemSnapshot modifierItem = productCatalog.findModifierItem(mod.modifierItemId())
                    .orElseThrow(() -> ApiException.badRequest("Modifier introuvable : " + mod.modifierItemId()));
            item.addModifier(modifierItem.id(), modifierItem.price(), mod.quantity() == null ? 1 : mod.quantity());
        }
    }

    /** Resolved names for a kitchen ticket line — unlike {@link #toItemDto}, no need to preserve unresolved/deleted modifiers. */
    private List<String> modifierNames(List<SendRoundRequest.ModifierLine> modifiers) {
        if (modifiers == null) {
            return List.of();
        }
        return modifiers.stream()
                .map(mod -> productCatalog.findModifierItem(mod.modifierItemId()).map(ProductCatalog.ModifierItemSnapshot::name).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean isVirtualTable(Long tableId) {
        return tableDirectory.findTable(tableId).map(TableDirectory.TableSnapshot::virtual).orElse(false);
    }

    private String tableNameOrNull(Long tableId) {
        return tableId == null ? null : tableDirectory.findTable(tableId).map(TableDirectory.TableSnapshot::name).orElse(null);
    }

    private String userNameOrNull(Long userId) {
        return userId == null ? null : userDirectory.namesByIds(Set.of(userId)).get(userId);
    }

    @Transactional
    public void finalizePayment(UUID orderUuid, FinalizePaymentRequest request, Long cashierUserId, String bearerToken) {
        Long sessionId = cashierFacade.findOpenSessionId(cashierUserId)
                .orElseThrow(() -> ApiException.forbidden("Caisse fermée"));

        Order order = orderRepository.findByUuid(orderUuid).orElseThrow(() -> ApiException.notFound("Commande introuvable."));

        if ("room_charge".equalsIgnoreCase(request.paymentMethod())) {
            // Billed to the guest's pms folio before touching any local state here:
            // if pms-modulith rejects or is unreachable, the order stays unpaid and
            // the cashier can retry, instead of silently losing the room charge.
            chargeToGuestRoom(order, request.roomNumber(), bearerToken);
        }

        CashierFacade.PaymentResult payment =
                cashierFacade.recordPayment(order.getId(), sessionId, request.paymentMethod(), order.getTotal(), request.amountReceived());

        order.markPaid(cashierUserId);
        orderRepository.save(order);

        commissionService.calculateCommissions(order);

        if (order.getTableId() != null && !isVirtualTable(order.getTableId())) {
            tableDirectory.markFree(order.getTableId());
        }

        events.publishEvent(toOrderPaidEvent(order, request.paymentMethod(), request.amountReceived(), payment.changeDue()));
    }

    /** Confirms the room is occupied, then bills this order's total to that stay's folio in pms-modulith. */
    private void chargeToGuestRoom(Order order, String roomNumber, String bearerToken) {
        if (roomNumber == null || roomNumber.isBlank()) {
            throw ApiException.badRequest("Le numéro de chambre est requis pour un paiement \"Chambre\".");
        }
        Long bookingId = pmsClient.checkRoom(roomNumber.trim(), bearerToken).bookingId();
        pmsClient.chargeToRoom(bookingId, order.getTotal(), order.getReference(), bearerToken);
    }

    /**
     * Standalone room lookup, called by the till BEFORE finalizing a "Chambre" payment so the
     * cashier can see and confirm the guest's name — {@link #finalizePayment} re-checks the room
     * itself right before billing (it can't trust a client-side confirmation from a prior call),
     * this is purely so a wrong/guessed room number is caught with a visible name mismatch instead
     * of silently billing whoever happens to be checked into that room.
     */
    @Transactional(readOnly = true)
    public PmsClient.RoomCheckResult checkGuestRoom(String roomNumber, String bearerToken) {
        if (roomNumber == null || roomNumber.isBlank()) {
            throw ApiException.badRequest("Le numéro de chambre est requis.");
        }
        return pmsClient.checkRoom(roomNumber.trim(), bearerToken);
    }

    /**
     * Re-publishes the same {@link OrderPaidEvent} {@link #finalizePayment} sent the first time,
     * rebuilt from the order's own persisted payment — {@code printing.application.PrintQueueListener}
     * reacts identically, queuing (and, for a network printer, immediately sending) a fresh receipt
     * print job. Doesn't touch the order/payment/table state at all, so it's safe to call any number
     * of times. Backs {@code POST /api/sales/{id}/reprint}.
     */
    @Transactional
    public void reprint(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> ApiException.notFound("Commande introuvable."));
        if (!order.isPaid()) {
            throw ApiException.conflict("Cette commande n'est pas payée : rien à réimprimer.");
        }
        CashierFacade.PaymentSnapshot payment = cashierFacade.findLatestPaymentForOrder(order.getId())
                .orElseThrow(() -> ApiException.notFound("Aucun paiement enregistré pour cette commande."));

        events.publishEvent(toOrderPaidEvent(order, payment.methodName(), payment.amountReceived(), payment.changeDue()));
    }

    /** Resolves everything {@code printing} needs to render a receipt — same names/tables resolution pattern as {@link #toDto}. */
    private OrderPaidEvent toOrderPaidEvent(Order order, String paymentMethod, BigDecimal amountReceived, BigDecimal changeDue) {
        List<OrderPaidEvent.RoundItems> rounds = order.getRounds().stream()
                .sorted(Comparator.comparingInt(OrderRound::getRoundNumber))
                .map(round -> new OrderPaidEvent.RoundItems(
                        round.getRoundNumber(), round.getItems().stream().map(this::toTicketItem).toList()))
                .toList();

        return new OrderPaidEvent(
                order.getId(), order.getUuid(), order.getBranchId(), order.getReference(),
                tableNameOrNull(order.getTableId()), userNameOrNull(order.getUserId()), rounds,
                order.getSubtotal(), order.getTax(), order.getDiscount(), order.getTotal(),
                paymentMethod, amountReceived, changeDue);
    }

    private OrderPaidEvent.TicketItem toTicketItem(OrderItem item) {
        String productName = productCatalog.findProduct(item.getProductId()).map(ProductCatalog.ProductSnapshot::name).orElse(null);
        List<String> modifierNames = item.getModifiers().stream()
                .map(m -> productCatalog.findModifierItem(m.getModifierItemId()).map(ProductCatalog.ModifierItemSnapshot::name).orElse(null))
                .filter(Objects::nonNull)
                .toList();
        return new OrderPaidEvent.TicketItem(productName, item.getQty(), item.getPrice(), item.getTotal(), modifierNames);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> history(Long cashierUserId) {
        Long sessionId = cashierFacade.findOpenSessionId(cashierUserId)
                .orElseThrow(() -> ApiException.badRequest("Session de caisse introuvable."));

        List<Order> paid = orderRepository.findByIdIn(cashierFacade.orderIdsPaidInSession(sessionId));
        List<Order> activeToday = orderRepository.findByStatusNotInAndCreatedAtAfter(
                List.of("cancelled", "paid"), LocalDate.now().atStartOfDay());

        Map<Long, Order> merged = new LinkedHashMap<>();
        Stream.concat(paid.stream(), activeToday.stream()).forEach(o -> merged.put(o.getId(), o));

        return merged.values().stream()
                .sorted(Comparator.comparing(Order::getId).reversed())
                .limit(50)
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> historyAdmin(String search, String status, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime from = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime to = endDate == null ? null : endDate.atTime(LocalTime.MAX);
        return orderRepository.search(blankToNull(search), blankToNull(status), from, to, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> index(String search, LocalDate date, String status, Pageable pageable) {
        LocalDateTime from = date == null ? null : date.atStartOfDay();
        LocalDateTime to = date == null ? null : date.atTime(LocalTime.MAX);
        String effectiveStatus = "all".equalsIgnoreCase(status) ? null : blankToNull(status);
        return orderRepository.search(blankToNull(search), effectiveStatus, from, to, pageable).map(this::toDto);
    }

    /** A virtual table never "resumes" a previous order — see {@link #resolveOrderForSendRound}. */
    @Transactional(readOnly = true)
    public Optional<OrderDto> getActiveOrder(Long tableId) {
        if (tableDirectory.findTable(tableId).map(TableDirectory.TableSnapshot::virtual).orElse(false)) {
            return Optional.empty();
        }
        return orderRepository.findFirstByTableIdAndStatusInOrderByIdDesc(tableId, ACTIVE_ORDER_STATUSES).map(this::toDto);
    }

    /**
     * Port of {@code TableController::transfer} — placed here, not in
     * {@code floorplan}, because it mutates {@code Order.tableId} and
     * {@code ordering} already depends on {@code floorplan} (through
     * {@link TableDirectory}); the reverse dependency would cycle. Laravel's
     * version calls a {@code $fromTable->activeOrder()} method that doesn't
     * exist anywhere in the codebase (only {@code currentOrder()} does) —
     * dead code fixed here by reusing the same "active order" lookup as
     * {@link #getActiveOrder}.
     */
    @Transactional
    public void transferTable(Long fromTableId, Long toTableId) {
        TableDirectory.TableSnapshot toTable = tableDirectory.findTable(toTableId)
                .orElseThrow(() -> ApiException.notFound("Table de destination introuvable."));
        if (toTable.virtual()) {
            throw ApiException.badRequest("Impossible de transférer une commande vers une table volante.");
        }
        if (!"free".equals(toTable.status())) {
            throw ApiException.badRequest("La table de destination est déjà occupée");
        }

        Order order = orderRepository.findFirstByTableIdAndStatusInOrderByIdDesc(fromTableId, ACTIVE_ORDER_STATUSES)
                .orElseThrow(() -> ApiException.notFound("Aucune commande active sur cette table"));

        order.transferToTable(toTableId);
        orderRepository.save(order);

        tableDirectory.markFree(fromTableId);
        tableDirectory.markOccupied(toTableId);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> waiterOrders(Long waiterUserId, LocalDate date, String status) {
        LocalDate effectiveDate = date == null ? LocalDate.now() : date;
        List<Order> orders = orderRepository.findByUserIdAndCreatedAtBetween(
                waiterUserId, effectiveDate.atStartOfDay(), effectiveDate.atTime(LocalTime.MAX));
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
            orders = orders.stream().filter(o -> status.equalsIgnoreCase(o.getStatus())).toList();
        }
        return orders.stream().sorted(Comparator.comparing(Order::getId).reversed()).map(this::toDto).toList();
    }

    @Transactional
    public OrderDto markAsServed(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> ApiException.notFound("Commande introuvable."));
        order.changeStatus("completed", userId);
        return toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto updateRoundItemQty(Long roundId, Long itemId, int newQty) {
        OrderRound round = orderRoundRepository.findById(roundId).orElseThrow(() -> ApiException.notFound("Round introuvable."));
        round.assertEditable();

        OrderItem item = round.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Cet article n'appartient pas à ce round."));

        int previousQty = item.getQty();
        if (newQty != previousQty) {
            if (newQty == 0) {
                round.removeItem(item);
            } else {
                item.updateQty(newQty);
                if (newQty > previousQty) {
                    notifyKitchenOfIncrease(round, item, newQty - previousQty);
                }
            }
            round.getOrder().refreshTotals();
        }

        orderRoundRepository.save(round);
        return toDto(orderRepository.save(round.getOrder()));
    }

    @Transactional
    public OrderDto addItemToRound(Long roundId, AddItemToRoundRequest request) {
        OrderRound round = orderRoundRepository.findById(roundId).orElseThrow(() -> ApiException.notFound("Round introuvable."));
        round.assertEditable();

        ProductCatalog.ProductSnapshot product = productCatalog.findProduct(request.productId())
                .orElseThrow(() -> ApiException.badRequest("Produit introuvable : " + request.productId()));

        OrderItem item = round.addItem(product.id(), null, request.qty(), product.price());
        addModifiers(item, request.modifiers());

        round.getOrder().refreshTotals();
        orderRoundRepository.save(round);
        Order order = orderRepository.save(round.getOrder());

        if (product.kitchenStationId() != null) {
            List<KitchenTicketRequestedEvent.TicketItem> items = List.of(
                    new KitchenTicketRequestedEvent.TicketItem(product.name(), request.qty(), modifierNames(request.modifiers())));
            events.publishEvent(new KitchenTicketRequestedEvent(
                    order.getId(), round.getId(), order.getBranchId(), product.kitchenStationId(), null,
                    tableNameOrNull(order.getTableId()), items, userNameOrNull(order.getUserId())));
        }

        return toDto(order);
    }

    private void notifyKitchenOfIncrease(OrderRound round, OrderItem item, int extraQty) {
        productCatalog.findProduct(item.getProductId()).ifPresent(product -> {
            if (product.kitchenStationId() != null) {
                String note = "Supplément : +%d %s".formatted(extraQty, product.name());
                List<KitchenTicketRequestedEvent.TicketItem> items = List.of(
                        new KitchenTicketRequestedEvent.TicketItem(product.name(), extraQty, List.of()));
                events.publishEvent(new KitchenTicketRequestedEvent(
                        round.getOrder().getId(), round.getId(), round.getOrder().getBranchId(), product.kitchenStationId(), note,
                        tableNameOrNull(round.getOrder().getTableId()), items, userNameOrNull(round.getOrder().getUserId())));
            }
        });
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    /** Public: also reused by {@link ReservationService} to render the order nested in a reservation. */
    public OrderDto toDto(Order order) {
        TableDirectory.TableSnapshot table = order.getTableId() == null ? null : tableDirectory.findTable(order.getTableId()).orElse(null);

        Set<Long> userIds = new LinkedHashSet<>();
        if (order.getUserId() != null) userIds.add(order.getUserId());
        if (order.getCashierId() != null) userIds.add(order.getCashierId());
        Map<Long, String> names = userDirectory.namesByIds(userIds);

        List<OrderDto.RoundDto> rounds = order.getRounds().stream()
                .sorted(Comparator.comparingInt(OrderRound::getRoundNumber))
                .map(this::toRoundDto)
                .toList();

        return new OrderDto(
                order.getId(),
                order.getUuid(),
                order.getReference(),
                order.getType(),
                order.getStatus(),
                new OrderDto.Amounts(order.getSubtotal(), order.getTax(), order.getDiscount(), order.getTotal(), formatFcfa(order.getTotal())),
                table == null ? null : new OrderDto.TableRef(table.id(), table.name(), table.status()),
                new OrderDto.PersonRef(order.getUserId(), order.getUserId() == null ? null : names.get(order.getUserId())),
                new OrderDto.PersonRef(order.getCashierId(), order.getCashierId() == null ? null : names.get(order.getCashierId())),
                rounds,
                order.getNote(),
                order.getCreatedAt() == null ? null : order.getCreatedAt().format(TIME),
                order.getCreatedAt() == null ? null : order.getCreatedAt().format(DATE)
        );
    }

    private OrderDto.RoundDto toRoundDto(OrderRound round) {
        List<OrderDto.ItemDto> items = round.getItems().stream().map(this::toItemDto).toList();
        return new OrderDto.RoundDto(
                round.getId(),
                round.getRoundNumber(),
                round.getStatus(),
                round.getNote(),
                round.getSentAt() == null ? null : round.getSentAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                items,
                round.totalRound()
        );
    }

    private OrderDto.ItemDto toItemDto(OrderItem item) {
        OrderDto.ProductRef product = productCatalog.findProduct(item.getProductId())
                .map(p -> new OrderDto.ProductRef(p.id(), p.name()))
                .orElse(new OrderDto.ProductRef(item.getProductId(), null));

        OrderDto.VariantRef variant = item.getVariantId() == null ? null : productCatalog.findVariant(item.getVariantId())
                .map(v -> new OrderDto.VariantRef(v.id(), v.name()))
                .orElse(null);

        List<OrderDto.ModifierDto> modifiers = item.getModifiers().stream()
                .map(m -> new OrderDto.ModifierDto(
                        m.getId(),
                        m.getModifierItemId(),
                        m.getQuantity(),
                        productCatalog.findModifierItem(m.getModifierItemId()).map(ProductCatalog.ModifierItemSnapshot::name).orElse(null),
                        m.getPrice()))
                .toList();

        return new OrderDto.ItemDto(
                item.getId(), item.getQty(), item.getPrice(), item.getTotal(), item.getStatus(),
                product, variant, modifiers, formatFcfa(item.getTotal()));
    }

    private String formatFcfa(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        DecimalFormat format = new DecimalFormat("#,###", symbols);
        return format.format(amount == null ? BigDecimal.ZERO : amount) + " FCFA";
    }
}
