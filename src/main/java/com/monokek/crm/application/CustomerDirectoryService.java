package com.monokek.crm.application;

import com.monokek.crm.CustomerDirectory;
import com.monokek.crm.domain.Customer;
import com.monokek.crm.domain.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
class CustomerDirectoryService implements CustomerDirectory {

    private final CustomerRepository customerRepository;

    CustomerDirectoryService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional
    public CustomerSnapshot findOrCreateByPhone(String phone, String name) {
        Customer customer = customerRepository.findByPhone(phone).stream().findFirst()
                .orElseGet(() -> {
                    Customer created = new Customer();
                    created.setPhone(phone);
                    created.setName(name);
                    return customerRepository.save(created);
                });
        return toSnapshot(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerSnapshot> findById(Long id) {
        return customerRepository.findById(id).map(this::toSnapshot);
    }

    private CustomerSnapshot toSnapshot(Customer customer) {
        return new CustomerSnapshot(customer.getId(), customer.getName(), customer.getPhone());
    }
}
