package com.monokek.cashier.infrastructure;

import com.monokek.cashier.domain.CashRegister;
import com.monokek.cashier.domain.CashRegisterRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaCashRegisterRepository extends CashRegisterRepository, JpaRepository<CashRegister, Long> {
}
