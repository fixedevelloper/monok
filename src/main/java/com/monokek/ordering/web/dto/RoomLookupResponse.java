package com.monokek.ordering.web.dto;

/** Response to a pre-payment "Chambre" lookup — the till shows {@code guestName} for the cashier to confirm before charging. */
public record RoomLookupResponse(Long bookingId, String guestName) {
}
