package com.monokek.cashier.infrastructure;

import com.monokek.cashier.domain.PaymentMethod;
import com.monokek.cashier.domain.PaymentMethodRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaPaymentMethodRepository extends PaymentMethodRepository, JpaRepository<PaymentMethod, Long> {
}
