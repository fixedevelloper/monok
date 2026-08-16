package com.monokek.staffing.web;

import com.monokek.identity.CurrentUser;
import com.monokek.staffing.application.ShiftService;
import com.monokek.staffing.web.dto.CreateShiftRequest;
import com.monokek.staffing.web.dto.ShiftDto;
import com.monokek.staffing.web.dto.UpdateShiftRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Roster planning — building the schedule is a management action, guarded by
 * {@code hasAnyRole("ADMIN", "MANAGER")} at the class level; {@link #mine}
 * is the one exception, open to any authenticated staff member checking
 * their own upcoming shifts.
 */
@RestController
@RequestMapping("/api/staffing/shifts")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class ShiftController {

    private final ShiftService shiftService;

    public ShiftController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    /** {@code branchId} query param stays optional but defaults to the caller's own branch when they're scoped to one, same rule as {@code printing.PrinterController#index}. */
    @GetMapping
    public List<ShiftDto> index(
            @RequestParam(required = false) Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @AuthenticationPrincipal CurrentUser principal) {
        return shiftService.list(principal.branchId() != null ? principal.branchId() : branchId, from, to);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/mine")
    public List<ShiftDto> mine(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @AuthenticationPrincipal CurrentUser principal) {
        return shiftService.listForUser(principal.id(), from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftDto store(@Valid @RequestBody CreateShiftRequest request, @AuthenticationPrincipal CurrentUser principal) {
        return shiftService.create(request, principal.branchId());
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ShiftDto update(@PathVariable Long id, @RequestBody UpdateShiftRequest request) {
        return shiftService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void destroy(@PathVariable Long id) {
        shiftService.delete(id);
    }
}
