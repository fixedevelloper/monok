package com.monokek.catalog.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface ModifierRepository extends Repository<Modifier, Long> {

    Modifier save(Modifier modifier);

    Optional<Modifier> findById(Long id);

    void deleteById(Long id);

    List<Modifier> findAll();
}
