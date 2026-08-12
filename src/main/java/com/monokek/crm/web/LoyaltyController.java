package com.monokek.crm.web;

import com.monokek.crm.application.LoyaltyService;
import com.monokek.crm.web.dto.LoyaltyPointsRequest;
import com.monokek.crm.web.dto.LoyaltyTransactionDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * New functionality (see {@code LoyaltyService}'s javadoc). Guarded by
 * {@code hasAnyRole("ADMIN", "MANAGER")} — see {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/admin/customers/{customerId}/loyalty")
/** Defense in depth: {@code SecurityConfig} already gates {@code /api/admin/**} to ADMIN/MANAGER — this is a second, method-level check that survives even if the path-based rule is ever misconfigured. */
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    public LoyaltyController(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    @GetMapping
    public List<LoyaltyTransactionDto> history(@PathVariable Long customerId) {
        return loyaltyService.history(customerId);
    }

    @PostMapping("/earn")
    public Map<String, Object> earn(@PathVariable Long customerId, @Valid @RequestBody LoyaltyPointsRequest request) {
        return Map.of("points", loyaltyService.earn(customerId, request.points()));
    }

    @PostMapping("/redeem")
    public Map<String, Object> redeem(@PathVariable Long customerId, @Valid @RequestBody LoyaltyPointsRequest request) {
        return Map.of("points", loyaltyService.redeem(customerId, request.points()));
    }
}
