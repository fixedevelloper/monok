package com.monokek.inventory.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** Mirrors {@code App\Http\Resources\RecipeResource}. {@code items} (not {@code ingredients}) to
 * stay symmetric with {@code RecipeRequest}'s own {@code items} — the admin "Fiche technique" form
 * reads this same response to pre-fill the edit form, not just to display it. {@code ingredientId}
 * is what makes that possible: without it the form has no way to know which ingredient a saved
 * line refers to, only its display name. */
public record RecipeDto(Long id, String productName, List<Line> items) {

    public record Line(Long ingredientId, String name, BigDecimal qty, String unit) {
    }
}
