package com.monokek.catalog.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface ModifierProductRepository extends Repository<ModifierProduct, Long> {

    ModifierProduct save(ModifierProduct modifierProduct);

    Optional<ModifierProduct> findById(Long id);

    void deleteById(Long id);

    List<ModifierProduct> findByProductId(Long productId);
}
