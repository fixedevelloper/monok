package com.monokek.inventory.infrastructure;

import com.monokek.inventory.domain.RecipeItem;
import com.monokek.inventory.domain.RecipeItemRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaRecipeItemRepository extends RecipeItemRepository, JpaRepository<RecipeItem, Long> {
}
