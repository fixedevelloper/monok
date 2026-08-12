package com.monokek.inventory.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recipes")
@Getter
@NoArgsConstructor
public class Recipe extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** References catalog.Product by id only — see module package-info. */
    @Column(name = "product_id")
    private Long productId;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RecipeItem> items = new ArrayList<>();

    public static Recipe forProduct(Long productId) {
        Recipe recipe = new Recipe();
        recipe.productId = productId;
        return recipe;
    }

    public RecipeItem addItem(Ingredient ingredient, BigDecimal qty) {
        RecipeItem item = RecipeItem.of(this, ingredient, qty);
        items.add(item);
        return item;
    }

    /** Port of {@code RecipeController::store}'s "delete then re-create" update strategy. */
    public void clearItems() {
        items.clear();
    }
}
