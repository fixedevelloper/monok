package com.monokek.company.web;

import com.monokek.company.application.WorkstationService;
import com.monokek.company.web.dto.CreateWorkstationRequest;
import com.monokek.company.web.dto.UpdateWorkstationRequest;
import com.monokek.company.web.dto.WorkstationDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * New functionality (see {@code CompanyService}'s javadoc). Guarded by
 * {@code hasAnyRole("ADMIN", "MANAGER")} — see {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/admin/workstations")
/** Defense in depth: {@code SecurityConfig} already gates {@code /api/admin/**} to ADMIN/MANAGER — this is a second, method-level check that survives even if the path-based rule is ever misconfigured. */
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class WorkstationController {

    private final WorkstationService workstationService;

    public WorkstationController(WorkstationService workstationService) {
        this.workstationService = workstationService;
    }

    @GetMapping
    public List<WorkstationDto> index(@RequestParam(required = false) Long branchId) {
        return workstationService.list(branchId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkstationDto store(@Valid @RequestBody CreateWorkstationRequest request) {
        return workstationService.create(request);
    }

    @GetMapping("/{id}")
    public WorkstationDto show(@PathVariable Long id) {
        return workstationService.show(id);
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public WorkstationDto update(@PathVariable Long id, @RequestBody UpdateWorkstationRequest request) {
        return workstationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void destroy(@PathVariable Long id) {
        workstationService.delete(id);
    }
}
