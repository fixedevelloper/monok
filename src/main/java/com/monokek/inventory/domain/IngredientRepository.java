package com.monokek.inventory.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface IngredientRepository extends Repository<Ingredient, Long> {

    Ingredient save(Ingredient ingredient);

    Optional<Ingredient> findById(Long id);

    List<Ingredient> findAll();

    boolean existsByName(String name);
}
