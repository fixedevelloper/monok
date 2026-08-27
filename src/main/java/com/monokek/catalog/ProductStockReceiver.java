package com.monokek.catalog;

import java.math.BigDecimal;

/**
 * Published interface: lets other modules mutate a storable product's stock count without
 * reaching into {@code catalog.domain} — deliberately separate from the read-only {@link
 * ProductCatalog} (an "open host service" documented as read-only) since this one mutates state.
 * Two callers today: {@code inventory} receives a supplier purchase into a storable product's
 * stock (e.g. crates of soda bought from a supplier, as opposed to a recipe ingredient);
 * {@code ordering} deducts a storable product's stock the moment an order selling it is paid
 * (the sibling case to {@code inventory.application.StockDeductionListener}, which does the same
 * for recipe ingredients — see {@code ordering.application.ProductStockDeductionListener}).
 */
public interface ProductStockReceiver {

    /** Increments {@code productId}'s stock by {@code qty}, updates its purchase price to {@code
     * unitPrice} (used for margin reporting), and logs a traced {@code ProductStockMovement} —
     * same shape as the admin "Ajuster le stock" modal, just triggered from a purchase order line
     * instead of a manual admin action. */
    void receivePurchase(Long productId, int qty, BigDecimal unitPrice, String reason, Long authorId);

    /** Decrements {@code productId}'s stock by {@code qty} sold and logs a traced {@code
     * ProductStockMovement} — a no-op for a non-"storable" product (consumable/service never carry
     * a real stock count), so callers never need to check the product's type themselves. Clamps at
     * zero instead of throwing on insufficient stock: by the time this runs the order is already
     * paid and committed, so there's nothing left to roll back — a movement reason noting the
     * clamp is a real signal (stock count is untrustworthy) rather than a swallowed failure. */
    void deductForSale(Long productId, int qty, Long orderId, Long cashierUserId);
}
