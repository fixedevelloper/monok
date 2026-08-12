package com.monokek.company.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface CompanyRepository extends Repository<Company, Long> {

    Company save(Company company);

    Optional<Company> findById(Long id);

    void deleteById(Long id);

    List<Company> findAll();
}
