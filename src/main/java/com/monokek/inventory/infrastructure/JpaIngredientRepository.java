package com.monokek.inventory.infrastructure;

import com.monokek.inventory.domain.Ingredient;
import com.monokek.inventory.domain.IngredientRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaIngredientRepository extends IngredientRepository, JpaRepository<Ingredient, Long> {
}
