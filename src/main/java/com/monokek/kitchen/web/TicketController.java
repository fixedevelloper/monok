package com.monokek.kitchen.web;

import com.monokek.identity.CurrentUser;
import com.monokek.kitchen.application.KitchenStationService;
import com.monokek.kitchen.application.KitchenTicketService;
import com.monokek.kitchen.web.dto.KitchenStationDto;
import com.monokek.kitchen.web.dto.KitchenTicketDto;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Port of {@code App\Http\Controllers\Api\Kitchen\TicketController}.
 * {@code /kitchen/tickets/{id}/status} and {@code /kitchen/tickets-direct/{id}/status}
 * both route to {@link KitchenTicketService#updateTicketStatus} — see its
 * javadoc for why the two Laravel methods were consolidated into one.
 */
@RestController
@RequestMapping("/api/kitchen")
public class TicketController {

    private final KitchenTicketService kitchenTicketService;
    private final KitchenStationService kitchenStationService;

    public TicketController(KitchenTicketService kitchenTicketService, KitchenStationService kitchenStationService) {
        this.kitchenTicketService = kitchenTicketService;
        this.kitchenStationService = kitchenStationService;
    }

    /** Scoped to the caller's own branch — see {@code floorplan.FloorService#list} for the same rule. */
    @GetMapping("/stations")
    public List<KitchenStationDto> getStations(@AuthenticationPrincipal CurrentUser principal) {
        return kitchenStationService.list(principal.branchId());
    }

    @GetMapping("/tickets")
    public List<KitchenTicketDto> index(@RequestParam Long stationId) {
        return kitchenTicketService.listStationTickets(stationId);
    }

    @PatchMapping("/tickets/{ticketId}/status")
    public Map<String, Object> updateStatus(@PathVariable Long ticketId, @RequestBody StatusRequest request) {
        String roundStatus = kitchenTicketService.updateTicketStatus(ticketId, request.status());
        return Map.of("message", "Statut mis à jour", "round_status", roundStatus);
    }

    @PatchMapping("/tickets-direct/{ticketId}/status")
    public Map<String, Object> updateStatusDirect(@PathVariable Long ticketId, @RequestBody StatusRequest request) {
        String roundStatus = kitchenTicketService.updateTicketStatus(ticketId, request.status());
        return Map.of("message", "Statut mis à jour", "round_status", roundStatus);
    }

    public record StatusRequest(@NotBlank String status) {
    }
}
