package com.monokek.kitchen.application;

import com.monokek.catalog.ProductCatalog;
import com.monokek.common.ApiException;
import com.monokek.kitchen.domain.KitchenTicket;
import com.monokek.kitchen.domain.KitchenTicketRepository;
import com.monokek.kitchen.web.dto.KitchenTicketDto;
import com.monokek.ordering.OrderRoundStatusUpdater;
import com.monokek.ordering.RoundKitchenView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Application service: port of {@code App\Http\Controllers\Api\Kitchen\TicketController}
 * (minus {@code updateItemStatus}, whose route in {@code routes/api.php} points at a
 * controller method that doesn't exist in the Laravel source — nothing to port).
 */
@Service
public class KitchenTicketService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> ACTIVE_STATUSES = List.of("pending", "preparing");
    private static final List<String> KNOWN_STATUSES = List.of("pending", "preparing", "ready", "served");

    private final KitchenTicketRepository ticketRepository;
    private final RoundKitchenView roundKitchenView;
    private final ProductCatalog productCatalog;
    private final OrderRoundStatusUpdater orderRoundStatusUpdater;

    public KitchenTicketService(
            KitchenTicketRepository ticketRepository,
            RoundKitchenView roundKitchenView,
            ProductCatalog productCatalog,
            OrderRoundStatusUpdater orderRoundStatusUpdater) {
        this.ticketRepository = ticketRepository;
        this.roundKitchenView = roundKitchenView;
        this.productCatalog = productCatalog;
        this.orderRoundStatusUpdater = orderRoundStatusUpdater;
    }

    @Transactional(readOnly = true)
    public List<KitchenTicketDto> listStationTickets(Long stationId) {
        return ticketRepository.findByStationIdAndStatusInOrderByCreatedAtAsc(stationId, ACTIVE_STATUSES).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Consolidates Laravel's {@code updateStatus} and {@code updateStatusDirect} (which had
     * diverged slightly — see {@code Order#applyKitchenRoundStatus}) into one implementation,
     * used by both {@code /kitchen/tickets/{id}/status} and {@code /kitchen/tickets-direct/{id}/status}.
     */
    @Transactional
    public String updateTicketStatus(Long ticketId, String newStatus) {
        if (!KNOWN_STATUSES.contains(newStatus)) {
            throw ApiException.badRequest("Statut inconnu : " + newStatus);
        }
        KitchenTicket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> ApiException.notFound("Ticket introuvable."));
        ticket.setStatus(newStatus);
        ticketRepository.save(ticket);

        List<KitchenTicket> roundTickets = ticketRepository.findByOrderRoundId(ticket.getOrderRoundId());
        String resolvedRoundStatus = resolveRoundStatus(roundTickets);

        orderRoundStatusUpdater.applyKitchenRoundStatus(ticket.getOrderId(), ticket.getOrderRoundId(), resolvedRoundStatus);
        return resolvedRoundStatus;
    }

    /**
     * Maps ticket-level statuses (pending/preparing/ready/served) onto the narrower round-level
     * vocabulary the {@code order_rounds.status} check constraint allows (pending/sent/preparing/
     * served — no "ready"). All ready/served → served; any ticket past pending (preparing OR
     * ready, e.g. a round with several tickets where only some are done) → preparing; otherwise
     * nothing has started yet → sent.
     *
     * <p>The previous fallback returned {@code roundTickets.get(0).getStatus()} verbatim, which
     * could be "ready" — not a valid round status — the moment a round had more than one ticket
     * and only some of them were marked ready: {@code CHECK constraint 'chk_order_rounds_status'
     * is violated} on the resulting update, discovered by actually finishing a ticket in a
     * multi-ticket round rather than just the always-single-ticket rounds every earlier test used.
     */
    private String resolveRoundStatus(List<KitchenTicket> roundTickets) {
        if (!roundTickets.isEmpty() && roundTickets.stream().allMatch(t -> "ready".equals(t.getStatus()) || "served".equals(t.getStatus()))) {
            return "served";
        }
        if (roundTickets.stream().anyMatch(t -> "preparing".equals(t.getStatus()) || "ready".equals(t.getStatus()))) {
            return "preparing";
        }
        return "sent";
    }

    private KitchenTicketDto toDto(KitchenTicket ticket) {
        RoundKitchenView.RoundSnapshot round = roundKitchenView.findRound(ticket.getOrderRoundId()).orElse(null);
        if (round == null) {
            return new KitchenTicketDto(ticket.getId(), "N/A", "Emporté", ticket.getStatus(), 1,
                    ticket.getCreatedAt() == null ? null : ticket.getCreatedAt().format(TIME), List.of());
        }

        List<KitchenTicketDto.ItemDto> items = round.items().stream().map(item -> {
            String productName = productCatalog.findProduct(item.productId()).map(ProductCatalog.ProductSnapshot::name).orElse("Produit inconnu");
            List<KitchenTicketDto.ModifierDto> modifiers = item.modifierItemIds().stream()
                    .map(id -> productCatalog.findModifierItem(id)
                            .map(m -> new KitchenTicketDto.ModifierDto(m.name(), 1))
                            .orElse(new KitchenTicketDto.ModifierDto("N/A", 1)))
                    .toList();
            return new KitchenTicketDto.ItemDto(item.id(), productName, item.qty(), modifiers);
        }).toList();

        return new KitchenTicketDto(
                ticket.getId(), round.orderReference(), round.tableName(), ticket.getStatus(), round.roundNumber(),
                ticket.getCreatedAt() == null ? null : ticket.getCreatedAt().format(TIME), items);
    }
}
