package com.monokek.reporting.web;

import com.monokek.reporting.application.StockMovementReportingService;
import com.monokek.reporting.web.dto.StockMovementPageDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Backs the admin "Historique des mouvements de stock" page — new functionality (not a Laravel
 * port), unifying {@code inventory}'s ingredient movements and {@code catalog}'s product
 * movements into one feed. Same {@code manage_stock} authority as every other stock-mutating/
 * stock-reading endpoint (ingredients, product stock adjust, purchase orders), not {@code
 * view_reports} — this is operational stock data, not a financial report.
 */
@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') and (hasRole('ADMIN') or hasAuthority('manage_stock'))")
public class StockMovementReportController {

    private final StockMovementReportingService stockMovementReportingService;

    public StockMovementReportController(StockMovementReportingService stockMovementReportingService) {
        this.stockMovementReportingService = stockMovementReportingService;
    }

    /** {@code size} is caller-controlled (not a fixed {@code @PageableDefault}) so the "Exporter en
     * PDF" button can request every row matching the current filters in one call (capped at 5000 —
     * generous for a stock ledger, and avoids an unbounded query if filters are left wide open). */
    @GetMapping("/api/admin/reporting/stock-movements")
    public StockMovementPageDto search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long ingredientId,
            @RequestParam(required = false) Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int cappedSize = Math.min(size, 5000);
        return stockMovementReportingService.search(search, type, startDate, endDate, ingredientId, productId, page, cappedSize);
    }
}
