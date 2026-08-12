package com.monokek.ordering.web;

import com.monokek.ordering.application.CommissionService;
import com.monokek.ordering.web.dto.CommissionDto;
import com.monokek.ordering.web.dto.CommissionStatsDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Port of {@code App\Http\Controllers\Api\Admin\CommissionController}.
 * Guarded by {@code hasAnyRole("ADMIN", "MANAGER")} — see {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/admin/commissions")
/** Defense in depth: {@code SecurityConfig} already gates {@code /api/admin/**} to ADMIN/MANAGER — this is a second, method-level check that survives even if the path-based rule is ever misconfigured. */
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class CommissionController {

    private final CommissionService commissionService;

    public CommissionController(CommissionService commissionService) {
        this.commissionService = commissionService;
    }

    @GetMapping
    public List<CommissionDto> index(@RequestParam(required = false) Integer month, @RequestParam(required = false) Integer year) {
        return commissionService.search(month, year);
    }

    @GetMapping("/stats")
    public CommissionStatsDto getStats() {
        return commissionService.stats();
    }

    @PostMapping("/settle/{waiterId}")
    public ResponseMessage settle(@PathVariable Long waiterId) {
        int settled = commissionService.settleWaiterCommissions(waiterId);
        return new ResponseMessage("Commissions réglées (" + settled + ")");
    }

    public record ResponseMessage(String message) {
    }
}
