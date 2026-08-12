package com.monokek.inventory.infrastructure;

import com.monokek.inventory.domain.StockMovement;
import com.monokek.inventory.domain.StockMovementRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaStockMovementRepository extends StockMovementRepository, JpaRepository<StockMovement, Long> {
}
