package com.monokek.crm.application;

import com.monokek.common.ApiException;
import com.monokek.crm.domain.Customer;
import com.monokek.crm.domain.CustomerRepository;
import com.monokek.crm.web.dto.CreateCustomerRequest;
import com.monokek.crm.web.dto.CustomerDto;
import com.monokek.crm.web.dto.UpdateCustomerRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * New functionality (see {@code Customer}'s javadoc for why this isn't a
 * Laravel port): basic admin management of customers.
 */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> list(String search) {
        List<Customer> customers = (search == null || search.isBlank())
                ? customerRepository.findAll()
                : customerRepository.findByNameContainingIgnoreCaseOrPhoneContaining(search, search);
        return customers.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CustomerDto show(Long id) {
        return toDto(findOrThrow(id));
    }

    @Transactional
    public CustomerDto create(CreateCustomerRequest request) {
        Customer customer = new Customer();
        customer.setName(request.name());
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        return toDto(customerRepository.save(customer));
    }

    @Transactional
    public CustomerDto update(Long id, UpdateCustomerRequest request) {
        Customer customer = findOrThrow(id);
        if (request.name() != null) customer.setName(request.name());
        if (request.phone() != null) customer.setPhone(request.phone());
        if (request.email() != null) customer.setEmail(request.email());
        return toDto(customerRepository.save(customer));
    }

    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        customerRepository.deleteById(id);
    }

    private Customer findOrThrow(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> ApiException.notFound("Client introuvable"));
    }

    private CustomerDto toDto(Customer customer) {
        return new CustomerDto(customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getPoints());
    }
}
