package com.monokek.inventory.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "recipe_items")
@Getter
@NoArgsConstructor
public class RecipeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(precision = 12, scale = 3)
    private BigDecimal qty;

    static RecipeItem of(Recipe recipe, Ingredient ingredient, BigDecimal qty) {
        RecipeItem item = new RecipeItem();
        item.recipe = recipe;
        item.ingredient = ingredient;
        item.qty = qty;
        return item;
    }
}
