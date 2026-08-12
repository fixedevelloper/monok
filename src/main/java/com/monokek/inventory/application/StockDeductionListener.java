package com.monokek.inventory.application;

import com.monokek.inventory.domain.Ingredient;
import com.monokek.inventory.domain.IngredientRepository;
import com.monokek.inventory.domain.Recipe;
import com.monokek.inventory.domain.RecipeItem;
import com.monokek.inventory.domain.RecipeRepository;
import com.monokek.inventory.domain.StockMovement;
import com.monokek.inventory.domain.StockMovementRepository;
import com.monokek.ordering.domain.event.OrderStatusChangedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Wires up what Laravel's {@code StockService::deductFromOrder} was clearly
 * meant to do — deduct recipe ingredients when an order is sold — but never
 * actually got called from anywhere (dead code: grep the Laravel source for
 * {@code StockService::} and nothing calls it). Reacts to {@code ordering}'s
 * own {@code OrderStatusChangedEvent} instead of {@code ordering} calling
 * into {@code inventory} directly, same one-directional shape as
 * {@code kitchen}/{@code settings}. Reads the sold items straight off the
 * event payload rather than calling back into {@code ordering} for them —
 * see {@code OrderStatusChangedEvent}'s own doc for why that second
 * dependency (a now-deleted {@code OrderLineItems} published interface) was
 * removed.
 */
@Component
public class StockDeductionListener {

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final StockMovementRepository stockMovementRepository;

    public StockDeductionListener(
            RecipeRepository recipeRepository,
            IngredientRepository ingredientRepository,
            StockMovementRepository stockMovementRepository) {
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    // @ApplicationModuleListener already runs after the publishing transaction commits
    // (it's a @TransactionalEventListener under the hood), so this handler needs its own
    // fresh transaction to make the deduction atomic — Spring rejects plain @Transactional
    // (default REQUIRED) here since there's no transaction left to join.
    @ApplicationModuleListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(OrderStatusChangedEvent event) {
        if (!"paid".equals(event.newStatus())) {
            return;
        }
        for (OrderStatusChangedEvent.SoldItem soldItem : event.items()) {
            recipeRepository.findByProductId(soldItem.productId())
                    .ifPresent(recipe -> deduct(recipe, soldItem.qty(), event.orderId()));
        }
    }

    private void deduct(Recipe recipe, int soldQty, Long orderId) {
        for (RecipeItem recipeItem : recipe.getItems()) {
            BigDecimal totalNeeded = recipeItem.getQty().multiply(BigDecimal.valueOf(soldQty));

            Ingredient ingredient = recipeItem.getIngredient();
            ingredient.applyMovement("out", totalNeeded);
            ingredientRepository.save(ingredient);

            StockMovement movement = new StockMovement();
            movement.setIngredient(ingredient);
            movement.setType("out");
            movement.setQty(totalNeeded);
            movement.setReason("Vente #" + orderId);
            stockMovementRepository.save(movement);
        }
    }
}
