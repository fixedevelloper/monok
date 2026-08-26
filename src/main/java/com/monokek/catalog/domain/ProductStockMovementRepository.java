package com.monokek.catalog.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;

@NoRepositoryBean
public interface ProductStockMovementRepository extends Repository<ProductStockMovement, Long> {

    ProductStockMovement save(ProductStockMovement movement);

    List<ProductStockMovement> findByProductIdOrderByIdDesc(Long productId);
}
