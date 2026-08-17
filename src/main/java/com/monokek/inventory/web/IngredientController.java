package com.monokek.inventory.web;

import com.monokek.inventory.application.IngredientService;
import com.monokek.inventory.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Port of {@code App\Http\Controllers\Api\Admin\IngredientController}.
 * Guarded by {@code hasAnyRole("ADMIN", "MANAGER")} — see {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/admin")
/** Defense in depth: {@code SecurityConfig} already gates {@code /api/admin/**} to ADMIN/MANAGER — this is a second, method-level check that survives even if the path-based rule is ever misconfigured. */
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @GetMapping("/ingredients")
    public List<IngredientDto> index() {
        return ingredientService.list();
    }

    @PostMapping("/ingredients")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') and (hasRole('ADMIN') or hasAuthority('manage_stock'))")
    public IngredientDto store(@Valid @RequestBody CreateIngredientRequest request) {
        return ingredientService.create(request);
    }

    @GetMapping("/units")
    public List<UnitDto> units() {
        return ingredientService.listUnits();
    }

    @GetMapping("/stock-movements")
    public Page<StockMovementDto> movements(@PageableDefault(size = 50) Pageable pageable) {
        return ingredientService.listMovements(pageable);
    }

    @PostMapping("/ingredients/{ingredientId}/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') and (hasRole('ADMIN') or hasAuthority('manage_stock'))")
    public Map<String, Object> adjustStock(@PathVariable Long ingredientId, @Valid @RequestBody AdjustStockRequest request) {
        BigDecimal newStock = ingredientService.adjustStock(ingredientId, request);
        return Map.of("message", "Stock mis à jour", "new_stock", newStock);
    }
}
