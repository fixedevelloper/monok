package com.monokek.crm.web;

import com.monokek.crm.application.CustomerService;
import com.monokek.crm.web.dto.CreateCustomerRequest;
import com.monokek.crm.web.dto.CustomerDto;
import com.monokek.crm.web.dto.UpdateCustomerRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * New functionality (see {@code com.monokek.crm.domain.Customer}'s javadoc):
 * admin customer management. Guarded by {@code hasAnyRole("ADMIN", "MANAGER")}
 * — see {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/admin/customers")
/** Defense in depth: {@code SecurityConfig} already gates {@code /api/admin/**} to ADMIN/MANAGER — this is a second, method-level check that survives even if the path-based rule is ever misconfigured. */
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public List<CustomerDto> index(@RequestParam(required = false) String search) {
        return customerService.list(search);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDto store(@Valid @RequestBody CreateCustomerRequest request) {
        return customerService.create(request);
    }

    @GetMapping("/{id}")
    public CustomerDto show(@PathVariable Long id) {
        return customerService.show(id);
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public CustomerDto update(@PathVariable Long id, @RequestBody UpdateCustomerRequest request) {
        return customerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void destroy(@PathVariable Long id) {
        customerService.delete(id);
    }
}
