package com.monokek.inventory.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** Mirrors {@code App\Http\Resources\RecipeResource}. */
public record RecipeDto(Long id, String productName, List<Line> ingredients) {

    public record Line(String name, BigDecimal qty, String unit) {
    }
}
