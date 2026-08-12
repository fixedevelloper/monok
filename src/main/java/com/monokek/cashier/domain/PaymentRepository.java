package com.monokek.cashier.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface PaymentRepository extends Repository<Payment, Long> {

    Payment save(Payment payment);

    Optional<Payment> findById(Long id);

    List<Payment> findByOrderId(Long orderId);

    List<Payment> findByCashSessionId(Long cashSessionId);
}
