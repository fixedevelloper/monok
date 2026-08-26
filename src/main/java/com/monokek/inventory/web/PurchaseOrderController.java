package com.monokek.inventory.web;

import com.monokek.common.ApiResponse;
import com.monokek.identity.CurrentUser;
import com.monokek.inventory.application.PurchaseOrderService;
import com.monokek.inventory.web.dto.CreatePurchaseOrderRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Port of {@code App\Http\Controllers\Api\Admin\PurchaseOrderController}.
 * Guarded by {@code hasAnyRole("ADMIN", "MANAGER")} — see {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/admin/purchase-orders")
/** Defense in depth: {@code SecurityConfig} already gates {@code /api/admin/**} to ADMIN/MANAGER — this is a second, method-level check that survives even if the path-based rule is ever misconfigured. */
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') and (hasRole('ADMIN') or hasAuthority('manage_stock'))")
    public ApiResponse<Void> store(@Valid @RequestBody CreatePurchaseOrderRequest request, @AuthenticationPrincipal CurrentUser principal) {
        purchaseOrderService.store(request, principal.id());
        return ApiResponse.message("Commande fournisseur enregistrée et stock mis à jour");
    }
}
