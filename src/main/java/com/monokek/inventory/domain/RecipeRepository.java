package com.monokek.inventory.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.Optional;

@NoRepositoryBean
public interface RecipeRepository extends Repository<Recipe, Long> {

    Recipe save(Recipe recipe);

    Optional<Recipe> findById(Long id);

    Optional<Recipe> findByProductId(Long productId);
}
