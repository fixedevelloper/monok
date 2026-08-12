package com.monokek.company.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface WorkstationRepository extends Repository<Workstation, Long> {

    Workstation save(Workstation workstation);

    Optional<Workstation> findById(Long id);

    void deleteById(Long id);

    List<Workstation> findAll();

    List<Workstation> findByBranchId(Long branchId);
}
