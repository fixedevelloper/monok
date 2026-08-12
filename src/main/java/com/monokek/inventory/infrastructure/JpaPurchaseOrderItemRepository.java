package com.monokek.inventory.infrastructure;

import com.monokek.inventory.domain.PurchaseOrderItem;
import com.monokek.inventory.domain.PurchaseOrderItemRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaPurchaseOrderItemRepository extends PurchaseOrderItemRepository, JpaRepository<PurchaseOrderItem, Long> {
}
