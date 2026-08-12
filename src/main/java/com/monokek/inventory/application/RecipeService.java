package com.monokek.inventory.application;

import com.monokek.catalog.ProductCatalog;
import com.monokek.common.ApiException;
import com.monokek.inventory.domain.Ingredient;
import com.monokek.inventory.domain.IngredientRepository;
import com.monokek.inventory.domain.Recipe;
import com.monokek.inventory.domain.RecipeRepository;
import com.monokek.inventory.web.dto.RecipeDto;
import com.monokek.inventory.web.dto.RecipeRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Application service: port of {@code App\Http\Controllers\Api\Admin\RecipeController}. */
@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final ProductCatalog productCatalog;

    public RecipeService(RecipeRepository recipeRepository, IngredientRepository ingredientRepository, ProductCatalog productCatalog) {
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.productCatalog = productCatalog;
    }

    @Transactional(readOnly = true)
    public Optional<RecipeDto> show(Long productId) {
        return recipeRepository.findByProductId(productId).map(this::toDto);
    }

    @Transactional
    public RecipeDto store(Long productId, RecipeRequest request) {
        productCatalog.findProduct(productId).orElseThrow(() -> ApiException.notFound("Produit introuvable."));

        Recipe recipe = recipeRepository.findByProductId(productId).orElseGet(() -> Recipe.forProduct(productId));
        recipe.clearItems();

        for (RecipeRequest.Line line : request.items()) {
            Ingredient ingredient = ingredientRepository.findById(line.ingredientId())
                    .orElseThrow(() -> ApiException.badRequest("Ingrédient introuvable : " + line.ingredientId()));
            recipe.addItem(ingredient, line.qty());
        }

        return toDto(recipeRepository.save(recipe));
    }

    private RecipeDto toDto(Recipe recipe) {
        String productName = productCatalog.findProduct(recipe.getProductId())
                .map(ProductCatalog.ProductSnapshot::name)
                .orElse(null);

        var lines = recipe.getItems().stream()
                .map(item -> new RecipeDto.Line(item.getIngredient().getName(), item.getQty(), item.getIngredient().getUnit().getName()))
                .toList();

        return new RecipeDto(recipe.getId(), productName, lines);
    }
}
