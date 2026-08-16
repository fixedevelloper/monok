package com.monokek.inventory.web;

import com.monokek.inventory.application.SupplierService;
import com.monokek.inventory.web.dto.CreateSupplierRequest;
import com.monokek.inventory.web.dto.SupplierDto;
import com.monokek.inventory.web.dto.UpdateSupplierRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin supplier directory management (contacts, coordinates) backing the
 * {@code Supplier} referenced by purchase orders. Guarded by
 * {@code hasAnyRole("ADMIN", "MANAGER")} — see {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/admin/suppliers")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public List<SupplierDto> index(@RequestParam(required = false) String search) {
        return supplierService.list(search);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierDto store(@Valid @RequestBody CreateSupplierRequest request) {
        return supplierService.create(request);
    }

    @GetMapping("/{id}")
    public SupplierDto show(@PathVariable Long id) {
        return supplierService.show(id);
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public SupplierDto update(@PathVariable Long id, @RequestBody UpdateSupplierRequest request) {
        return supplierService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void destroy(@PathVariable Long id) {
        supplierService.delete(id);
    }
}
