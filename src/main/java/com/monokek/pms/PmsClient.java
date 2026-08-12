package com.monokek.pms;

import java.math.BigDecimal;

/**
 * Public entry point into the {@code pms} module for the rest of the
 * application. Other modules must depend only on this interface, never on
 * {@code com.monokek.pms.internal} types.
 */
public interface PmsClient {

    /**
     * Confirms the room is occupied by a checked-in guest in pms-modulith and
     * returns the booking id to bill charges against.
     *
     * @throws com.monokek.common.ApiException notFound if the room isn't occupied
     *         (or doesn't exist), serviceUnavailable if pms-modulith can't be reached
     */
    Long checkRoomAndGetBookingId(String roomNumber, String bearerToken);

    /** Bills one restaurant charge to the room's folio for the given booking. */
    void chargeToRoom(Long bookingId, BigDecimal amount, String orderReference, String bearerToken);
}
