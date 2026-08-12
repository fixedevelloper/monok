package com.monokek.company.web;

import com.monokek.company.application.CompanyService;
import com.monokek.company.web.dto.CompanyDto;
import com.monokek.company.web.dto.CreateCompanyRequest;
import com.monokek.company.web.dto.UpdateCompanyRequest;
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
@RequestMapping("/api/admin/companies")
/** Defense in depth: {@code SecurityConfig} already gates {@code /api/admin/**} to ADMIN/MANAGER — this is a second, method-level check that survives even if the path-based rule is ever misconfigured. */
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public List<CompanyDto> index() {
        return companyService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyDto store(@Valid @RequestBody CreateCompanyRequest request) {
        return companyService.create(request);
    }

    @GetMapping("/{id}")
    public CompanyDto show(@PathVariable Long id) {
        return companyService.show(id);
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public CompanyDto update(@PathVariable Long id, @RequestBody UpdateCompanyRequest request) {
        return companyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void destroy(@PathVariable Long id) {
        companyService.delete(id);
    }
}
