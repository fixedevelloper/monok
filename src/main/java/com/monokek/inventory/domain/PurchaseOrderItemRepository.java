package com.monokek.inventory.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface PurchaseOrderItemRepository extends Repository<PurchaseOrderItem, Long> {

    PurchaseOrderItem save(PurchaseOrderItem item);

    Optional<PurchaseOrderItem> findById(Long id);

    List<PurchaseOrderItem> findByPurchaseOrderId(Long purchaseOrderId);
}
