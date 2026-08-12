package com.monokek.kitchen.web.dto;

import java.util.List;

/**
 * Mirrors {@code App\Http\Resources\KitchenTicketResource}. As in Laravel,
 * {@code items} is every item of the round, not filtered to this ticket's
 * station — {@code KitchenTicketResource} reads {@code $this->round->items}
 * directly rather than the (broken — it filters on a column that doesn't
 * exist on {@code order_items}) {@code KitchenTicket::items()} relation.
 */
public record KitchenTicketDto(
        Long id, String reference, String table, String status, int roundNumber, String createdAt, List<ItemDto> items) {

    public record ItemDto(Long id, String name, int qty, List<ModifierDto> modifiers) {
    }

    public record ModifierDto(String name, int quantity) {
    }
}
