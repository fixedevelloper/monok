package com.monokek.cashier.infrastructure;

import com.monokek.cashier.domain.CashSession;
import com.monokek.cashier.domain.CashSessionRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaCashSessionRepository extends CashSessionRepository, JpaRepository<CashSession, Long> {
}
