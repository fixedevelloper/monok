package com.monokek.pms.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Mirrors pms-modulith's {@code PosService.RoomCheckResult} wire format (snake_case, see its Jackson config). */
record RoomCheckResponse(
        @JsonProperty("booking_id") Long bookingId,
        @JsonProperty("guest_name") String guestName,
        @JsonProperty("room_id") Long roomId) {
}
