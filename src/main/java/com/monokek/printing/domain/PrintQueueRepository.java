package com.monokek.printing.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface PrintQueueRepository extends Repository<PrintQueue, Long> {

    PrintQueue save(PrintQueue job);

    Optional<PrintQueue> findById(Long id);

    List<PrintQueue> findByStatus(String status);

    List<PrintQueue> findByStatusAndAttemptsLessThan(String status, int maxAttempts);

    List<PrintQueue> findByStatusAndPrinter_BranchId(String status, Long branchId);
}
