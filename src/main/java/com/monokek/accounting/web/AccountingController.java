package com.monokek.accounting.web;

import com.monokek.accounting.application.AccountingService;
import com.monokek.accounting.application.ExcelReportWriter;
import com.monokek.accounting.application.PdfReportWriter;
import com.monokek.accounting.application.ReportTable;
import com.monokek.common.ApiException;
import com.monokek.identity.CurrentUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Backs {@code AccountingPage.tsx}'s four "Excel (.xlsx)" / "Imprimer PDF"
 * buttons — the endpoint they've always called ({@code /api/accounting/*})
 * simply didn't exist server-side. Moved under {@code /api/admin/accounting/**}
 * (the frontend now calls that path too) so it inherits the standard
 * {@code hasAnyRole("ADMIN", "MANAGER")} rule in {@code SecurityConfig}
 * instead of needing its own carve-out — financial exports are exactly the
 * kind of endpoint that must never be reachable by a waiter/cashier token.
 */
@RestController
@RequestMapping("/api/admin/accounting")
/** Defense in depth: {@code SecurityConfig} already gates {@code /api/admin/**} to ADMIN/MANAGER — this is a second, method-level check that survives even if the path-based rule is ever misconfigured. */
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AccountingController {

    private final AccountingService accountingService;
    private final ExcelReportWriter excelReportWriter;
    private final PdfReportWriter pdfReportWriter;

    public AccountingController(AccountingService accountingService, ExcelReportWriter excelReportWriter, PdfReportWriter pdfReportWriter) {
        this.accountingService = accountingService;
        this.excelReportWriter = excelReportWriter;
        this.pdfReportWriter = pdfReportWriter;
    }

    /** {@code branchId} is client-supplied (an unscoped owner picks which branch's tab it's
     * exporting); a branch-scoped manager can't override it with someone else's branch — same
     * effective-branch pattern as {@code ProductController#indexPage}. */
    @GetMapping("/{report}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') and (hasRole('ADMIN') or hasAuthority('view_reports'))")
    public ResponseEntity<byte[]> export(
            @PathVariable String report,
            @RequestParam("start_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("end_date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam String format,
            @RequestParam(required = false) Long branchId,
            @AuthenticationPrincipal CurrentUser principal) {
        Long effectiveBranchId = principal.branchId() != null ? principal.branchId() : branchId;
        ReportTable table = accountingService.build(report, startDate, endDate, effectiveBranchId);
        return render(table, format, report + "_" + startDate + "_au_" + endDate);
    }

    /** One cash-register shift's orders — {@code sessionId} alone identifies the branch (via its
     * register), so unlike {@link #export} there's no separate client-supplied {@code branchId}
     * to override: a branch-scoped manager can only ever reach their own branch's sessions
     * (enforced in {@code AccountingService#buildForSession}), an unscoped owner reaches any. */
    @GetMapping("/shift/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') and (hasRole('ADMIN') or hasAuthority('view_reports'))")
    public ResponseEntity<byte[]> exportShift(
            @PathVariable Long sessionId,
            @RequestParam String format,
            @AuthenticationPrincipal CurrentUser principal) {
        ReportTable table = accountingService.buildForSession(sessionId, principal.branchId());
        return render(table, format, "shift_" + sessionId);
    }

    private ResponseEntity<byte[]> render(ReportTable table, String format, String filenameBase) {
        byte[] body;
        MediaType contentType;
        String extension;
        if ("excel".equals(format)) {
            body = excelReportWriter.write(table);
            contentType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            extension = "xlsx";
        } else if ("pdf".equals(format)) {
            body = pdfReportWriter.write(table);
            contentType = MediaType.APPLICATION_PDF;
            extension = "pdf";
        } else {
            throw ApiException.badRequest("Format d'export inconnu : " + format);
        }

        String filename = filenameBase + "." + extension;
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(body);
    }
}
