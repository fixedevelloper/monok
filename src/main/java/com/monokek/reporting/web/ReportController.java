package com.monokek.reporting.web;

import com.monokek.identity.CurrentUser;
import com.monokek.reporting.application.ReportingService;
import com.monokek.reporting.web.dto.AnalyticsResponse;
import com.monokek.reporting.web.dto.CategorySales;
import com.monokek.reporting.web.dto.ClosingReportResponse;
import com.monokek.reporting.web.dto.DashboardStatsResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Port of {@code App\Http\Controllers\Api\Admin\ReportController}. Routes
 * match Laravel's {@code routes/api.php} exactly (all under the
 * {@code role:admin|manager} group, see {@code SecurityConfig}):
 * {@code GET /admin/reports/dashboard}, {@code /admin/reports/categories},
 * {@code /admin/analytics}, {@code /admin/reports/closing}.
 */
@RestController
/** Defense in depth: {@code SecurityConfig} already gates {@code /api/admin/**} to ADMIN/MANAGER — this is a second, method-level check that survives even if the path-based rule is ever misconfigured. */
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class ReportController {

    private final ReportingService reportingService;

    public ReportController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/api/admin/reports/dashboard")
    public DashboardStatsResponse dashboardStats() {
        return reportingService.dashboardStats();
    }

    @GetMapping("/api/admin/reports/categories")
    public List<CategorySales> salesByCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return reportingService.salesByCategory(startDate, endDate);
    }

    @GetMapping("/api/admin/reports/closing")
    public ClosingReportResponse closingReport(@AuthenticationPrincipal CurrentUser currentUser) {
        return reportingService.closingReport(currentUser.id(), currentUser.name());
    }

    @GetMapping("/api/admin/analytics")
    public AnalyticsResponse getAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return reportingService.getAnalytics(startDate, endDate);
    }
}
