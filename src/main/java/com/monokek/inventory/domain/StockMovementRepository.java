package com.monokek.inventory.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface StockMovementRepository extends Repository<StockMovement, Long> {

    StockMovement save(StockMovement movement);

    Optional<StockMovement> findById(Long id);

    List<StockMovement> findByIngredientId(Long ingredientId);

    Page<StockMovement> findAllByOrderByIdDesc(Pageable pageable);
}
