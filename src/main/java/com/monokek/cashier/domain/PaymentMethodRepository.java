package com.monokek.cashier.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface PaymentMethodRepository extends Repository<PaymentMethod, Long> {

    PaymentMethod save(PaymentMethod method);

    Optional<PaymentMethod> findById(Long id);

    List<PaymentMethod> findAll();
}
