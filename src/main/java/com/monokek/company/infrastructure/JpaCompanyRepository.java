package com.monokek.company.infrastructure;

import com.monokek.company.domain.Company;
import com.monokek.company.domain.CompanyRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaCompanyRepository extends CompanyRepository, JpaRepository<Company, Long> {
}
