package com.monokek.crm.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface LoyaltyTransactionRepository extends Repository<LoyaltyTransaction, Long> {

    LoyaltyTransaction save(LoyaltyTransaction transaction);

    Optional<LoyaltyTransaction> findById(Long id);

    List<LoyaltyTransaction> findByCustomerId(Long customerId);
}
