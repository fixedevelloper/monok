package com.monokek.crm.infrastructure;

import com.monokek.crm.domain.Customer;
import com.monokek.crm.domain.CustomerRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaCustomerRepository extends CustomerRepository, JpaRepository<Customer, Long> {
}
