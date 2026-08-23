package com.monokek.ordering.web;

import com.monokek.common.ApiResponse;
import com.monokek.identity.CurrentUser;
import com.monokek.ordering.application.OrderService;
import com.monokek.ordering.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Port of {@code App\Http\Controllers\Api\Pos\OrderController}.
 */
@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/api/pos/orders/send-round")
    public SendRoundResult sendRound(@Valid @RequestBody SendRoundRequest request, @AuthenticationPrincipal CurrentUser principal) {
        return orderService.sendRound(request, principal.id(), principal.id());
    }

    @PostMapping("/api/pos/orders/{uuid}/finalize")
    public ApiResponse<Void> finalizePayment(
            @PathVariable UUID uuid, @Valid @RequestBody FinalizePaymentRequest request,
            @AuthenticationPrincipal CurrentUser principal,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        orderService.finalizePayment(uuid, request, principal.id(), authorization);
        return ApiResponse.success(null, "Payé");
    }

    /** Pre-payment lookup for a "Chambre" (room charge) payment — see {@code OrderService#checkGuestRoom}. */
    @GetMapping("/api/pos/rooms/{roomNumber}")
    public ApiResponse<RoomLookupResponse> checkRoom(
            @PathVariable String roomNumber, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        var result = orderService.checkGuestRoom(roomNumber, authorization);
        return ApiResponse.success(new RoomLookupResponse(result.bookingId(), result.guestName()));
    }

    /**
     * Voids an unpaid order. Open to any authenticated till user like every other
     * {@code /api/pos/**} action — the actual gate is {@code request.managerPin()}, verified inside
     * {@code OrderService#cancelOrder} against monokek-identity: an unsupervised void is a classic
     * POS fraud vector (ring up cash, pocket it, void the order), so it's always a manager-approved
     * override now rather than something a caller's own role alone can unlock.
     */
    @PostMapping("/api/pos/orders/{uuid}/cancel")
    public ApiResponse<Void> cancelOrder(
            @PathVariable UUID uuid, @Valid @RequestBody CancelOrderRequest request,
            @AuthenticationPrincipal CurrentUser principal,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        orderService.cancelOrder(uuid, request.reason(), request.managerPin(), principal.id(), principal.branchId(), authorization);
        return ApiResponse.message("Commande annulée");
    }

    /** Removes one round from a still-open order — same manager-PIN override as {@link #cancelOrder}, at round granularity. */
    @PostMapping("/api/pos/rounds/{roundId}/void")
    public ApiResponse<OrderDto> voidRound(
            @PathVariable Long roundId, @Valid @RequestBody VoidRoundRequest request,
            @AuthenticationPrincipal CurrentUser principal,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        OrderDto order = orderService.voidRound(roundId, request.reason(), request.managerPin(), principal.id(), principal.branchId(), authorization);
        return ApiResponse.success(order, "Round supprimé");
    }

    @GetMapping("/api/pos/orders")
    public Page<OrderDto> index(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal CurrentUser principal,
            @PageableDefault(size = 15) Pageable pageable) {
        return orderService.index(search, date, status, principal.branchId(), pageable);
    }

    @GetMapping("/api/pos/orders/history")
    public ApiResponse<List<OrderDto>> history(@AuthenticationPrincipal CurrentUser principal) {
        return ApiResponse.success(orderService.history(principal.id(), principal.branchId()));
    }

    /** {@code branchId} is client-supplied (an unscoped owner picks which branch's tab it's
     * viewing); a branch-scoped manager can't override it with someone else's branch — same
     * effective-branch pattern as {@code ProductController#indexPage}. */
    @GetMapping("/api/admin/orders/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ApiResponse<Page<OrderDto>> historyAdmin(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Long branchId,
            @AuthenticationPrincipal CurrentUser principal,
            @PageableDefault(size = 50) Pageable pageable) {
        Long effectiveBranchId = principal.branchId() != null ? principal.branchId() : branchId;
        return ApiResponse.success(orderService.historyAdmin(search, status, startDate, endDate, effectiveBranchId, pageable));
    }

    @GetMapping("/api/pos/waiter/orders")
    public List<OrderDto> waiterOrders(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal CurrentUser principal) {
        return orderService.waiterOrders(principal.id(), date, status);
    }

    @GetMapping("/api/pos/tables/{tableId}/active-order")
    public ApiResponse<OrderDto> getActiveOrder(@PathVariable Long tableId) {
        return orderService.getActiveOrder(tableId)
                .map(order -> ApiResponse.success(order, (String) null))
                .orElseGet(() -> ApiResponse.success(null, "Aucune commande active pour cette table"));
    }

    @PostMapping("/api/pos/orders/{orderId}/serve")
    public ApiResponse<Void> markAsServed(@PathVariable Long orderId, @AuthenticationPrincipal CurrentUser principal) {
        orderService.markAsServed(orderId, principal.id());
        return ApiResponse.message("Commande servie !");
    }

    @PatchMapping("/api/pos/rounds/{roundId}/items/{itemId}")
    public OrderDto updateRoundItemQty(
            @PathVariable Long roundId, @PathVariable Long itemId, @Valid @RequestBody UpdateRoundItemQtyRequest request) {
        return orderService.updateRoundItemQty(roundId, itemId, request.qty());
    }

    @PostMapping("/api/pos/rounds/{roundId}/items")
    public OrderDto addItemToRound(@PathVariable Long roundId, @Valid @RequestBody AddItemToRoundRequest request) {
        return orderService.addItemToRound(roundId, request);
    }

    /** Route kept at its original Laravel path (under {@code /pos/tables}) even though it lives in this controller — see {@code OrderService#transferTable}. */
    @PostMapping("/api/pos/tables/transfer")
    public ApiResponse<Void> transferTable(@Valid @RequestBody TransferTableRequest request) {
        orderService.transferTable(request.fromTableId(), request.toTableId());
        return ApiResponse.message("Commande transférée");
    }

    /**
     * Re-queues a receipt print job for an already-paid order — kept at its original
     * frontend-facing path ({@code /api/sales/{id}/reprint}, note: no {@code /pos} prefix)
     * even though it lives in {@code ordering}, same reasoning as {@code transferTable}
     * above. Requires a valid bearer token now (see {@code SecurityConfig}) — it used to
     * be {@code permitAll()} for a route that didn't exist yet.
     */
    @PostMapping("/api/sales/{id}/reprint")
    public ApiResponse<Void> reprint(@PathVariable Long id) {
        orderService.reprint(id);
        return ApiResponse.message("Réimpression envoyée");
    }
}
