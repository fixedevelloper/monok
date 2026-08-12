package com.monokek.inventory.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface RecipeItemRepository extends Repository<RecipeItem, Long> {

    RecipeItem save(RecipeItem item);

    Optional<RecipeItem> findById(Long id);

    List<RecipeItem> findByRecipeId(Long recipeId);
}
