package com.monokek.staffing.web;

import com.monokek.common.ApiException;
import com.monokek.staffing.application.TimeClockService;
import com.monokek.staffing.web.dto.ClockInRequest;
import com.monokek.staffing.web.dto.ClockOutRequest;
import com.monokek.staffing.web.dto.TimeClockEntryDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Self-service clock-in/out for whoever is operating a shared POS terminal —
 * {@code userId} is already resolved client-side via monokek-identity's PIN
 * lookup (see the module's package-info), so no role restriction beyond
 * "authenticated" applies here, unlike {@link ShiftController}. {@link #history}
 * is the one management/reporting endpoint, guarded accordingly.
 */
@RestController
@RequestMapping("/api/staffing/clock")
public class TimeClockController {

    private final TimeClockService timeClockService;

    public TimeClockController(TimeClockService timeClockService) {
        this.timeClockService = timeClockService;
    }

    @PostMapping("/clock-in")
    @ResponseStatus(HttpStatus.CREATED)
    public TimeClockEntryDto clockIn(@Valid @RequestBody ClockInRequest request) {
        return timeClockService.clockIn(request.userId(), request.branchId());
    }

    @PostMapping("/clock-out")
    public TimeClockEntryDto clockOut(@Valid @RequestBody ClockOutRequest request) {
        return timeClockService.clockOut(request.userId());
    }

    /** Who's currently clocked in at this branch. */
    @GetMapping("/current")
    public List<TimeClockEntryDto> current(@RequestParam Long branchId) {
        return timeClockService.currentlyPresent(branchId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/history")
    public List<TimeClockEntryDto> history(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        if (userId != null) return timeClockService.historyForUser(userId, from, to);
        if (branchId != null) return timeClockService.historyForBranch(branchId, from, to);
        throw ApiException.badRequest("userId ou branchId requis");
    }
}
