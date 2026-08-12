package com.monokek.ordering.web.dto;

/** Mirrors {@code App\Http\Resources\ReservationResource}. */
public record ReservationDto(
        Long id,
        String pickupDate,
        int guestsCount,
        String managerNotes,
        String reservationStatus,
        CustomerRef customer,
        OrderDto order,
        String createdAt,
        String updatedAt
) {
    public record CustomerRef(Long id, String name, String phone) {
    }
}
