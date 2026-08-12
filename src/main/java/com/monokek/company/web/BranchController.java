package com.monokek.company.web;

import com.monokek.company.application.BranchService;
import com.monokek.company.web.dto.BranchDto;
import com.monokek.company.web.dto.CreateBranchRequest;
import com.monokek.company.web.dto.UpdateBranchRequest;
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
@RequestMapping("/api/admin/branches")
/** Defense in depth: {@code SecurityConfig} already gates {@code /api/admin/**} to ADMIN/MANAGER — this is a second, method-level check that survives even if the path-based rule is ever misconfigured. */
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping
    public List<BranchDto> index(@RequestParam(required = false) Long companyId) {
        return branchService.list(companyId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BranchDto store(@Valid @RequestBody CreateBranchRequest request) {
        return branchService.create(request);
    }

    @GetMapping("/{id}")
    public BranchDto show(@PathVariable Long id) {
        return branchService.show(id);
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public BranchDto update(@PathVariable Long id, @RequestBody UpdateBranchRequest request) {
        return branchService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void destroy(@PathVariable Long id) {
        branchService.delete(id);
    }
}
