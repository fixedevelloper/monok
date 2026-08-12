package com.monokek.company.application;

import com.monokek.common.ApiException;
import com.monokek.company.domain.Branch;
import com.monokek.company.domain.BranchRepository;
import com.monokek.company.domain.Company;
import com.monokek.company.domain.CompanyRepository;
import com.monokek.company.domain.event.BranchCreatedEvent;
import com.monokek.company.web.dto.BranchDto;
import com.monokek.company.web.dto.CreateBranchRequest;
import com.monokek.company.web.dto.UpdateBranchRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** New functionality — see {@code CompanyService}'s javadoc. */
@Service
public class BranchService {

    private final BranchRepository branchRepository;
    private final CompanyRepository companyRepository;
    private final ApplicationEventPublisher events;

    public BranchService(BranchRepository branchRepository, CompanyRepository companyRepository, ApplicationEventPublisher events) {
        this.branchRepository = branchRepository;
        this.companyRepository = companyRepository;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<BranchDto> list(Long companyId) {
        List<Branch> branches = companyId == null ? branchRepository.findAll() : branchRepository.findByCompanyId(companyId);
        return branches.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public BranchDto show(Long id) {
        return toDto(findOrThrow(id));
    }

    @Transactional
    public BranchDto create(CreateBranchRequest request) {
        Company company = findCompanyOrThrow(request.companyId());

        Branch branch = new Branch();
        branch.setCompany(company);
        branch.setName(request.name());
        branch.setAddress(request.address());
        branch.setPhone(request.phone());
        branch = branchRepository.save(branch);
        events.publishEvent(new BranchCreatedEvent(branch.getId(), branch.getName()));
        return toDto(branch);
    }

    @Transactional
    public BranchDto update(Long id, UpdateBranchRequest request) {
        Branch branch = findOrThrow(id);
        if (request.companyId() != null) branch.setCompany(findCompanyOrThrow(request.companyId()));
        if (request.name() != null) branch.setName(request.name());
        if (request.address() != null) branch.setAddress(request.address());
        if (request.phone() != null) branch.setPhone(request.phone());
        return toDto(branchRepository.save(branch));
    }

    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        // workstations.branch_id is ON DELETE CASCADE at the schema level.
        branchRepository.deleteById(id);
    }

    private Branch findOrThrow(Long id) {
        return branchRepository.findById(id).orElseThrow(() -> ApiException.notFound("Succursale introuvable"));
    }

    private Company findCompanyOrThrow(Long companyId) {
        return companyRepository.findById(companyId).orElseThrow(() -> ApiException.badRequest("Société introuvable"));
    }

    private BranchDto toDto(Branch branch) {
        return new BranchDto(branch.getId(), branch.getCompany().getId(), branch.getCompany().getName(),
                branch.getName(), branch.getAddress(), branch.getPhone());
    }
}
