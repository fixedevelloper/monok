package com.monokek.inventory.infrastructure;

import com.monokek.inventory.domain.Supplier;
import com.monokek.inventory.domain.SupplierRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaSupplierRepository extends SupplierRepository, JpaRepository<Supplier, Long> {
}
