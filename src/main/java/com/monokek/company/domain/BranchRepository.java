package com.monokek.company.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface BranchRepository extends Repository<Branch, Long> {

    Branch save(Branch branch);

    Optional<Branch> findById(Long id);

    void deleteById(Long id);

    List<Branch> findAll();

    List<Branch> findByCompanyId(Long companyId);
}
