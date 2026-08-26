package com.monokek.catalog;

import java.math.BigDecimal;

/**
 * Published interface: {@code inventory} needs to receive a supplier purchase into a storable
 * product's stock (e.g. crates of soda bought from a supplier, as opposed to a recipe ingredient)
 * without reaching into {@code catalog.domain} — deliberately separate from the read-only {@link
 * ProductCatalog} (an "open host service" documented as read-only) since this one mutates state.
 */
public interface ProductStockReceiver {

    /** Increments {@code productId}'s stock by {@code qty}, updates its purchase price to {@code
     * unitPrice} (used for margin reporting), and logs a traced {@code ProductStockMovement} —
     * same shape as the admin "Ajuster le stock" modal, just triggered from a purchase order line
     * instead of a manual admin action. */
    void receivePurchase(Long productId, int qty, BigDecimal unitPrice, String reason, Long authorId);
}
