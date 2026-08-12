package com.monokek.cashier.web;

import com.monokek.cashier.application.CashierService;
import com.monokek.cashier.web.dto.*;
import com.monokek.identity.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Port of {@code App\Http\Controllers\Api\Pos\CashController}.
 */
@RestController
@RequestMapping("/api/cash")
public class CashController {

    private final CashierService cashierService;

    public CashController(CashierService cashierService) {
        this.cashierService = cashierService;
    }

    /** Scoped to the caller's own branch — see {@link CashierService#listRegisters}. */
    @GetMapping("/registers")
    public List<CashRegisterDto> listRegisters(@AuthenticationPrincipal CurrentUser principal) {
        return cashierService.listRegisters(principal.branchId());
    }

    /**
     * Creating a cash register is till-configuration, not a day-to-day POS action — unlike every
     * other endpoint in this controller, it must not be reachable by a plain cashier/waiter token.
     * Flagged in the security audit: this used to be {@code authenticated()}-only, the one gap in
     * an otherwise-consistent "config is admin-only" rule across branches/printers/workstations/etc.
     */
    @PostMapping("/registers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public CashRegisterDto storeRegister(@Valid @RequestBody StoreRegisterRequest request, @AuthenticationPrincipal CurrentUser principal) {
        return cashierService.storeRegister(request, principal.branchId());
    }

    /** Till configuration, same reasoning as {@link #storeRegister} above. */
    @RequestMapping(value = "/registers/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public CashRegisterDto updateRegister(@PathVariable Long id, @RequestBody UpdateRegisterRequest request) {
        return cashierService.updateRegister(id, request);
    }

    /** Till configuration, same reasoning as {@link #storeRegister} above. */
    @DeleteMapping("/registers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void destroyRegister(@PathVariable Long id) {
        cashierService.deleteRegister(id);
    }

    @GetMapping("/status")
    public CashStatusDto status(@AuthenticationPrincipal CurrentUser principal) {
        return cashierService.status(principal.id());
    }

    @PostMapping("/open")
    @ResponseStatus(HttpStatus.CREATED)
    public CashSessionDto open(@Valid @RequestBody OpenSessionRequest request, @AuthenticationPrincipal CurrentUser principal) {
        return cashierService.open(request, principal.id());
    }

    @GetMapping("/current-summary")
    public CashSummaryDto currentSummary(@AuthenticationPrincipal CurrentUser principal) {
        return cashierService.currentSummary(principal.id());
    }

    @PostMapping("/close")
    public CloseReportDto close(@Valid @RequestBody CloseSessionRequest request, @AuthenticationPrincipal CurrentUser principal) {
        return cashierService.close(request, principal.id());
    }
}
