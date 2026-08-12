package com.monokek.catalog.infrastructure;

import com.monokek.catalog.domain.ProductVariant;
import com.monokek.catalog.domain.ProductVariantRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaProductVariantRepository extends ProductVariantRepository, JpaRepository<ProductVariant, Long> {
}
