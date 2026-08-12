package com.monokek.inventory.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface UnitRepository extends Repository<Unit, Long> {

    Unit save(Unit unit);

    Optional<Unit> findById(Long id);

    List<Unit> findAll();
}
