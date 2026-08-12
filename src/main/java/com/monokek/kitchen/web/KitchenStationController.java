package com.monokek.kitchen.web;

import com.monokek.identity.CurrentUser;
import com.monokek.kitchen.application.KitchenStationService;
import com.monokek.kitchen.web.dto.CreateKitchenStationRequest;
import com.monokek.kitchen.web.dto.KitchenStationDto;
import com.monokek.kitchen.web.dto.UpdateKitchenStationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin CRUD for kitchen stations — didn't exist at all before (stations could only be
 * seeded directly in the database). Guarded by {@code hasAnyRole("ADMIN", "MANAGER")} —
 * see {@code SecurityConfig}. The read-only, kitchen/pos-facing {@code GET /api/kitchen/stations}
 * (with each station's pending-ticket count) stays on {@code TicketController}.
 */
@RestController
@RequestMapping("/api/admin/kitchen-stations")
/** Defense in depth: {@code SecurityConfig} already gates {@code /api/admin/**} to ADMIN/MANAGER — this is a second, method-level check that survives even if the path-based rule is ever misconfigured. */
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class KitchenStationController {

    private final KitchenStationService kitchenStationService;

    public KitchenStationController(KitchenStationService kitchenStationService) {
        this.kitchenStationService = kitchenStationService;
    }

    @GetMapping
    public List<KitchenStationDto> index(@RequestParam(required = false) Long branchId) {
        return kitchenStationService.list(branchId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public KitchenStationDto store(@Valid @RequestBody CreateKitchenStationRequest request, @AuthenticationPrincipal CurrentUser principal) {
        return kitchenStationService.create(request, principal.branchId());
    }

    @GetMapping("/{id}")
    public KitchenStationDto show(@PathVariable Long id) {
        return kitchenStationService.show(id);
    }

    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public KitchenStationDto update(@PathVariable Long id, @RequestBody UpdateKitchenStationRequest request) {
        return kitchenStationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void destroy(@PathVariable Long id) {
        kitchenStationService.delete(id);
    }
}
