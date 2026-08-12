package com.monokek.crm.infrastructure;

import com.monokek.crm.domain.LoyaltyTransaction;
import com.monokek.crm.domain.LoyaltyTransactionRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaLoyaltyTransactionRepository extends LoyaltyTransactionRepository, JpaRepository<LoyaltyTransaction, Long> {
}
