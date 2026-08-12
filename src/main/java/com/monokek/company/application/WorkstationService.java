package com.monokek.company.application;

import com.monokek.common.ApiException;
import com.monokek.company.domain.Branch;
import com.monokek.company.domain.BranchRepository;
import com.monokek.company.domain.Workstation;
import com.monokek.company.domain.WorkstationRepository;
import com.monokek.company.web.dto.CreateWorkstationRequest;
import com.monokek.company.web.dto.UpdateWorkstationRequest;
import com.monokek.company.web.dto.WorkstationDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** New functionality — see {@code CompanyService}'s javadoc. */
@Service
public class WorkstationService {

    private final WorkstationRepository workstationRepository;
    private final BranchRepository branchRepository;

    public WorkstationService(WorkstationRepository workstationRepository, BranchRepository branchRepository) {
        this.workstationRepository = workstationRepository;
        this.branchRepository = branchRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkstationDto> list(Long branchId) {
        List<Workstation> workstations = branchId == null ? workstationRepository.findAll() : workstationRepository.findByBranchId(branchId);
        return workstations.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public WorkstationDto show(Long id) {
        return toDto(findOrThrow(id));
    }

    @Transactional
    public WorkstationDto create(CreateWorkstationRequest request) {
        Branch branch = findBranchOrThrow(request.branchId());

        Workstation workstation = new Workstation();
        workstation.setBranch(branch);
        workstation.setName(request.name());
        workstation.setType(request.type());
        workstation.setIp(request.ip());
        return toDto(workstationRepository.save(workstation));
    }

    @Transactional
    public WorkstationDto update(Long id, UpdateWorkstationRequest request) {
        Workstation workstation = findOrThrow(id);
        if (request.branchId() != null) workstation.setBranch(findBranchOrThrow(request.branchId()));
        if (request.name() != null) workstation.setName(request.name());
        if (request.type() != null) workstation.setType(request.type());
        if (request.ip() != null) workstation.setIp(request.ip());
        return toDto(workstationRepository.save(workstation));
    }

    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        workstationRepository.deleteById(id);
    }

    private Workstation findOrThrow(Long id) {
        return workstationRepository.findById(id).orElseThrow(() -> ApiException.notFound("Poste de travail introuvable"));
    }

    private Branch findBranchOrThrow(Long branchId) {
        return branchRepository.findById(branchId).orElseThrow(() -> ApiException.badRequest("Succursale introuvable"));
    }

    private WorkstationDto toDto(Workstation workstation) {
        return new WorkstationDto(workstation.getId(), workstation.getBranch().getId(), workstation.getBranch().getName(),
                workstation.getName(), workstation.getType(), workstation.getIp());
    }
}
