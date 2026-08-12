package com.monokek.inventory.infrastructure;

import com.monokek.inventory.domain.Recipe;
import com.monokek.inventory.domain.RecipeRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaRecipeRepository extends RecipeRepository, JpaRepository<Recipe, Long> {
}
