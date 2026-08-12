package com.monokek.catalog.infrastructure;

import com.monokek.catalog.domain.Product;
import com.monokek.catalog.domain.ProductRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaProductRepository extends ProductRepository, JpaRepository<Product, Long> {
}
