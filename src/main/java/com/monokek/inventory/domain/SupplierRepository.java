package com.monokek.inventory.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface SupplierRepository extends Repository<Supplier, Long> {

    Supplier save(Supplier supplier);

    Optional<Supplier> findById(Long id);

    void deleteById(Long id);

    List<Supplier> findAll();

    List<Supplier> findByNameContainingIgnoreCaseOrPhoneContaining(String name, String phone);
}
