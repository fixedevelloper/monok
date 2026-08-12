package com.monokek.inventory.infrastructure;

import com.monokek.inventory.domain.PurchaseOrder;
import com.monokek.inventory.domain.PurchaseOrderRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaPurchaseOrderRepository extends PurchaseOrderRepository, JpaRepository<PurchaseOrder, Long> {
}
