/**
 * Inventory module: units, ingredients, recipes, suppliers, purchase orders
 * and stock movements. Implemented end-to-end. Depends one-directionally on
 * {@code ordering}'s {@code domain.event} named interface and on
 * {@code catalog} ({@code ProductCatalog}, to resolve a product's name for
 * recipe responses) — same shape as {@code kitchen}: neither {@code ordering}
 * nor {@code catalog} ever depends back on {@code inventory}.
 *
 * <p>{@code application.StockDeductionListener} reacts to
 * {@code OrderStatusChangedEvent} to deduct recipe ingredients when an order
 * is paid — the feature Laravel's {@code StockService::deductFromOrder}
 * was clearly meant to provide but that nothing in the source app ever
 * actually called. It reads the sold items straight off the event's
 * {@code items()} payload rather than calling back into {@code ordering} for
 * them — {@code ordering} used to publish a second, narrow read-model
 * interface ({@code OrderLineItems}) just for this, which meant
 * {@code inventory} depended on {@code ordering} two different ways for one
 * piece of information. Folding the sold-item list into the event itself
 * collapsed that down to a single dependency: the event type it already has
 * to know about to listen for it at all.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Inventory")
package com.monokek.inventory;
