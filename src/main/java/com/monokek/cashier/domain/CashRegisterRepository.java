package com.monokek.cashier.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface CashRegisterRepository extends Repository<CashRegister, Long> {

    CashRegister save(CashRegister register);

    Optional<CashRegister> findById(Long id);

    void deleteById(Long id);

    List<CashRegister> findAll();

    List<CashRegister> findByBranchId(Long branchId);
}
