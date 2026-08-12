package com.monokek.inventory.web;

import com.monokek.inventory.application.RecipeService;
import com.monokek.inventory.web.dto.RecipeDto;
import com.monokek.inventory.web.dto.RecipeRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Port of {@code App\Http\Controllers\Api\Admin\RecipeController}.
 * Guarded by {@code hasAnyRole("ADMIN", "MANAGER")} — see {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/admin/products/{productId}/recipe")
/** Defense in depth: {@code SecurityConfig} already gates {@code /api/admin/**} to ADMIN/MANAGER — this is a second, method-level check that survives even if the path-based rule is ever misconfigured. */
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public Object show(@PathVariable Long productId) {
        return recipeService.show(productId)
                .map(recipe -> (Object) recipe)
                .orElseGet(() -> Map.of("message", "Aucune recette définie", "items", List.of()));
    }

    @PostMapping
    public RecipeDto store(@PathVariable Long productId, @Valid @RequestBody RecipeRequest request) {
        return recipeService.store(productId, request);
    }
}
