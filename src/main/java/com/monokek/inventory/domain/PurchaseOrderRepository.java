package com.monokek.inventory.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface PurchaseOrderRepository extends Repository<PurchaseOrder, Long> {

    PurchaseOrder save(PurchaseOrder purchaseOrder);

    Optional<PurchaseOrder> findById(Long id);

    List<PurchaseOrder> findAll();

    List<PurchaseOrder> findBySupplierIdOrderByIdDesc(Long supplierId);
}
