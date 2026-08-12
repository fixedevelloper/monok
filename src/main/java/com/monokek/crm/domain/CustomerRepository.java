package com.monokek.crm.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface CustomerRepository extends Repository<Customer, Long> {

    Customer save(Customer customer);

    Optional<Customer> findById(Long id);

    void deleteById(Long id);

    List<Customer> findAll();

    List<Customer> findByPhone(String phone);

    List<Customer> findByNameContainingIgnoreCaseOrPhoneContaining(String name, String phone);
}
