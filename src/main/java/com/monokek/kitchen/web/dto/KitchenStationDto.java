package com.monokek.kitchen.web.dto;

import com.monokek.kitchen.domain.StationType;

/** Mirrors {@code TicketController::getStations}' inline array shape, plus branchId/type for admin CRUD. */
public record KitchenStationDto(Long id, Long branchId, String name, StationType type, long pendingTicketsCount) {
}
