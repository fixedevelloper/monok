package com.monokek.kitchen;

import java.util.Optional;

/**
 * Published interface: lets {@code printing} route a kitchen ticket to the
 * printer matching its station's actual type (kitchen, bar, grill...)
 * instead of every station's ticket being printed on the branch's single
 * {@code "kitchen"}-location printer, without {@code printing} reaching into
 * {@code kitchen.domain}.
 */
public interface KitchenStationDirectory {

    /** Lowercased {@link com.monokek.kitchen.domain.StationType} name (e.g. {@code "bar"}), matching
     * {@code printing.domain.Printer#location} — empty if the station was deleted. */
    Optional<String> findPrinterLocation(Long stationId);
}
