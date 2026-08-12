package com.monokek.company.application;

import com.monokek.common.ApiException;
import com.monokek.company.domain.Company;
import com.monokek.company.domain.CompanyRepository;
import com.monokek.company.web.dto.CompanyDto;
import com.monokek.company.web.dto.CreateCompanyRequest;
import com.monokek.company.web.dto.UpdateCompanyRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * New functionality — Laravel has a {@code companies} table and a bare
 * model, but no {@code CompanyController} anywhere (see {@code company}'s
 * package-info). Admin management of the multi-site hierarchy every ported
 * module already references by id (`branchId` in `ordering`, `catalog`,
 * `kitchen`, `cashier`, `printing`...).
 */
@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public List<CompanyDto> list() {
        return companyRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CompanyDto show(Long id) {
        return toDto(findOrThrow(id));
    }

    @Transactional
    public CompanyDto create(CreateCompanyRequest request) {
        Company company = new Company();
        company.setName(request.name());
        company.setPhone(request.phone());
        company.setEmail(request.email());
        return toDto(companyRepository.save(company));
    }

    @Transactional
    public CompanyDto update(Long id, UpdateCompanyRequest request) {
        Company company = findOrThrow(id);
        if (request.name() != null) company.setName(request.name());
        if (request.phone() != null) company.setPhone(request.phone());
        if (request.email() != null) company.setEmail(request.email());
        return toDto(companyRepository.save(company));
    }

    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        // branches.company_id is ON DELETE CASCADE at the schema level, same as Laravel's migration.
        companyRepository.deleteById(id);
    }

    private Company findOrThrow(Long id) {
        return companyRepository.findById(id).orElseThrow(() -> ApiException.notFound("Société introuvable"));
    }

    private CompanyDto toDto(Company company) {
        return new CompanyDto(company.getId(), company.getName(), company.getPhone(), company.getEmail());
    }
}
