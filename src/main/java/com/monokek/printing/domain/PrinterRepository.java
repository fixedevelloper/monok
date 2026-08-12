package com.monokek.printing.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface PrinterRepository extends Repository<Printer, Long> {

    Printer save(Printer printer);

    Optional<Printer> findById(Long id);

    void deleteById(Long id);

    List<Printer> findAll();

    List<Printer> findByBranchId(Long branchId);

    Optional<Printer> findFirstByBranchIdAndLocationAndActiveTrue(Long branchId, String location);
}
