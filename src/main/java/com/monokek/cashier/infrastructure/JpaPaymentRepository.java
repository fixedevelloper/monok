package com.monokek.cashier.infrastructure;

import com.monokek.cashier.domain.Payment;
import com.monokek.cashier.domain.PaymentRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaPaymentRepository extends PaymentRepository, JpaRepository<Payment, Long> {
}
