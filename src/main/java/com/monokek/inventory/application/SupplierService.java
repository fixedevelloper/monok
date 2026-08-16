package com.monokek.inventory.application;

import com.monokek.common.ApiException;
import com.monokek.inventory.domain.Supplier;
import com.monokek.inventory.domain.SupplierRepository;
import com.monokek.inventory.web.dto.CreateSupplierRequest;
import com.monokek.inventory.web.dto.SupplierDto;
import com.monokek.inventory.web.dto.UpdateSupplierRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Admin management of the supplier directory used by purchase orders. */
@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public List<SupplierDto> list(String search) {
        List<Supplier> suppliers = (search == null || search.isBlank())
                ? supplierRepository.findAll()
                : supplierRepository.findByNameContainingIgnoreCaseOrPhoneContaining(search, search);
        return suppliers.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public SupplierDto show(Long id) {
        return toDto(findOrThrow(id));
    }

    @Transactional
    public SupplierDto create(CreateSupplierRequest request) {
        Supplier supplier = new Supplier();
        supplier.setName(request.name());
        supplier.setPhone(request.phone());
        supplier.setEmail(request.email());
        supplier.setAddress(request.address());
        supplier.setContactName(request.contactName());
        return toDto(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierDto update(Long id, UpdateSupplierRequest request) {
        Supplier supplier = findOrThrow(id);
        if (request.name() != null) supplier.setName(request.name());
        if (request.phone() != null) supplier.setPhone(request.phone());
        if (request.email() != null) supplier.setEmail(request.email());
        if (request.address() != null) supplier.setAddress(request.address());
        if (request.contactName() != null) supplier.setContactName(request.contactName());
        return toDto(supplierRepository.save(supplier));
    }

    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        supplierRepository.deleteById(id);
    }

    private Supplier findOrThrow(Long id) {
        return supplierRepository.findById(id).orElseThrow(() -> ApiException.notFound("Fournisseur introuvable"));
    }

    private SupplierDto toDto(Supplier supplier) {
        return new SupplierDto(supplier.getId(), supplier.getName(), supplier.getPhone(), supplier.getEmail(), supplier.getAddress(), supplier.getContactName());
    }
}
