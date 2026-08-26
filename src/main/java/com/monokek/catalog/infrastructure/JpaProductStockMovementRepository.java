package com.monokek.catalog.infrastructure;

import com.monokek.catalog.domain.ProductStockMovement;
import com.monokek.catalog.domain.ProductStockMovementRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaProductStockMovementRepository extends ProductStockMovementRepository, JpaRepository<ProductStockMovement, Long> {
}
