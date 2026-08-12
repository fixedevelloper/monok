package com.monokek.kitchen.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface KitchenStationRepository extends Repository<KitchenStation, Long> {

    KitchenStation save(KitchenStation station);

    Optional<KitchenStation> findById(Long id);

    void deleteById(Long id);

    List<KitchenStation> findAll();

    List<KitchenStation> findByBranchId(Long branchId);

    /** Used by the POS's "Comptoir Bar" screen to find its station without a hardcoded id. */
    Optional<KitchenStation> findFirstByBranchIdAndType(Long branchId, StationType type);
}
