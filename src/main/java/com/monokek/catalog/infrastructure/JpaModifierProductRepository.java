package com.monokek.catalog.infrastructure;

import com.monokek.catalog.domain.ModifierProduct;
import com.monokek.catalog.domain.ModifierProductRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaModifierProductRepository extends ModifierProductRepository, JpaRepository<ModifierProduct, Long> {
}
