package com.monokek.printing.web;

import com.monokek.identity.CurrentUser;
import com.monokek.printing.application.PrinterService;
import com.monokek.printing.web.dto.CreatePrinterRequest;
import com.monokek.printing.web.dto.PrinterDto;
import com.monokek.printing.web.dto.TestConnectionResult;
import com.monokek.printing.web.dto.UpdatePrinterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Port of {@code App\Http\Controllers\Api\Admin\PrinterController}.
 * Guarded by {@code hasAnyRole("ADMIN", "MANAGER")} — see {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/admin/settings/printers")
/** Defense in depth: {@code SecurityConfig} already gates {@code /api/admin/**} to ADMIN/MANAGER — this is a second, method-level check that survives even if the path-based rule is ever misconfigured. */
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class PrinterController {

    private final PrinterService printerService;

    public PrinterController(PrinterService printerService) {
        this.printerService = printerService;
    }

    /** {@code branchId} query param stays optional (admins managing several branches may pass one
     * explicitly) but defaults to the caller's own branch when they're scoped to one, same rule
     * as {@code floorplan.FloorService#list}. */
    @GetMapping
    public List<PrinterDto> index(@RequestParam(required = false) Long branchId, @AuthenticationPrincipal CurrentUser principal) {
        return printerService.list(principal.branchId() != null ? principal.branchId() : branchId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrinterDto store(@Valid @RequestBody CreatePrinterRequest request, @AuthenticationPrincipal CurrentUser principal) {
        return printerService.store(request, principal.branchId());
    }

    @GetMapping("/{id}")
    public PrinterDto show(@PathVariable Long id) {
        return printerService.show(id);
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public PrinterDto update(@PathVariable Long id, @RequestBody UpdatePrinterRequest request) {
        return printerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void destroy(@PathVariable Long id) {
        printerService.destroy(id);
    }

    @GetMapping("/{id}/test")
    public TestConnectionResult testConnection(@PathVariable Long id) {
        return printerService.testConnection(id);
    }
}
