package com.monokek.kitchen.application;

import com.monokek.kitchen.domain.KitchenStation;
import com.monokek.kitchen.domain.KitchenStationRepository;
import com.monokek.kitchen.domain.KitchenTicket;
import com.monokek.kitchen.domain.KitchenTicketRepository;
import com.monokek.ordering.domain.event.KitchenTicketRequestedEvent;
import com.monokek.ordering.domain.event.OrderStatusChangedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to {@code ordering}'s domain events instead of {@code ordering}
 * reaching into {@code kitchen_tickets} directly — replaces the
 * {@code KitchenTicket::create(...)} calls Laravel makes straight from
 * {@code OrderController} plus the {@code $order->kitchenTickets()->update(...)}
 * call {@code markAsServed} makes across what would otherwise be a module
 * boundary.
 */
@Component
public class KitchenTicketListener {

    private final KitchenTicketRepository kitchenTicketRepository;
    private final KitchenStationRepository kitchenStationRepository;

    public KitchenTicketListener(KitchenTicketRepository kitchenTicketRepository, KitchenStationRepository kitchenStationRepository) {
        this.kitchenTicketRepository = kitchenTicketRepository;
        this.kitchenStationRepository = kitchenStationRepository;
    }

    @ApplicationModuleListener
    void on(KitchenTicketRequestedEvent event) {
        KitchenStation station = kitchenStationRepository.findById(event.stationId()).orElse(null);
        if (station == null) {
            return; // station was deleted between the order being placed and the ticket being created
        }

        KitchenTicket ticket = new KitchenTicket();
        ticket.setOrderId(event.orderId());
        ticket.setOrderRoundId(event.roundId());
        ticket.setStation(station);
        ticket.setNote(event.note());
        ticket.setStatus("pending");
        kitchenTicketRepository.save(ticket);
    }

    @ApplicationModuleListener
    void on(OrderStatusChangedEvent event) {
        if (!"completed".equals(event.newStatus())) {
            return;
        }
        for (KitchenTicket ticket : kitchenTicketRepository.findByOrderId(event.orderId())) {
            ticket.setStatus("served");
            kitchenTicketRepository.save(ticket);
        }
    }
}
