package com.monokek.ordering.web;

import com.monokek.common.ApiResponse;
import com.monokek.identity.CurrentUser;
import com.monokek.ordering.application.ReservationService;
import com.monokek.ordering.web.dto.CreateReservationRequest;
import com.monokek.ordering.web.dto.ReservationDto;
import com.monokek.ordering.web.dto.UpdateReservationRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Port of {@code App\Http\Controllers\Api\Admin\ReservationController}.
 * Guarded by {@code hasAnyRole("ADMIN", "MANAGER")} — see {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/admin/reservations")
/** Defense in depth: {@code SecurityConfig} already gates {@code /api/admin/**} to ADMIN/MANAGER — this is a second, method-level check that survives even if the path-based rule is ever misconfigured. */
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public Page<ReservationDto> index(@RequestParam(required = false) String filter, @PageableDefault(size = 20) Pageable pageable) {
        return reservationService.index(filter, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReservationDto> store(@Valid @RequestBody CreateReservationRequest request, @AuthenticationPrincipal CurrentUser principal) {
        return ApiResponse.success(reservationService.create(request, principal.id()));
    }

    @PostMapping("/{orderId}/pay")
    public ApiResponse<Void> pay(@PathVariable Long orderId, @AuthenticationPrincipal CurrentUser principal) {
        reservationService.pay(orderId, principal.id());
        return ApiResponse.message("Paiement validé");
    }

    @PutMapping("/{reservationId}")
    public ApiResponse<ReservationDto> update(@PathVariable Long reservationId, @Valid @RequestBody UpdateReservationRequest request) {
        return ApiResponse.success(reservationService.update(reservationId, request), "Réservation mise à jour avec succès");
    }

    @DeleteMapping("/{reservationId}")
    public ApiResponse<Void> destroy(@PathVariable Long reservationId) {
        reservationService.destroy(reservationId);
        return ApiResponse.message("Réservation supprimée");
    }
}
