package com.monokek.catalog.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface ModifierItemRepository extends Repository<ModifierItem, Long> {

    ModifierItem save(ModifierItem item);

    Optional<ModifierItem> findById(Long id);

    void deleteById(Long id);

    List<ModifierItem> findByModifierId(Long modifierId);
}
