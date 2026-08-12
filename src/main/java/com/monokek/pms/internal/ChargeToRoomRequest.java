package com.monokek.pms.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/** Mirrors pms-modulith's {@code PosRequests.ChargeToRoomRequest} wire format (snake_case, see its Jackson config). */
record ChargeToRoomRequest(
        @JsonProperty("booking_id") Long bookingId,
        String department,
        @JsonProperty("item_name") String itemName,
        Integer quantity,
        @JsonProperty("unit_price") BigDecimal unitPrice,
        @JsonProperty("external_order_id") String externalOrderId) {
}
