package com.monokek.catalog.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface ProductVariantRepository extends Repository<ProductVariant, Long> {

    ProductVariant save(ProductVariant variant);

    Optional<ProductVariant> findById(Long id);

    List<ProductVariant> findByProductId(Long productId);
}
