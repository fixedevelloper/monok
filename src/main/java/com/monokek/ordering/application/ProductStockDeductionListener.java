package com.monokek.ordering.application;

import com.monokek.catalog.ProductStockReceiver;
import com.monokek.ordering.domain.event.OrderStatusChangedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sibling of {@code inventory.application.StockDeductionListener}, for the other kind of sellable
 * item: a directly-stocked {@code catalog.domain.Product} (e.g. a bottled drink), as opposed to a
 * prepared dish whose recipe consumes {@code inventory.Ingredient}s. Lives in {@code ordering}
 * rather than {@code catalog} — {@code catalog} has no dependency on {@code ordering} (it's the
 * other way around, via {@link com.monokek.catalog.ProductCatalog}), so a listener reacting to
 * {@code ordering}'s own event has to sit on this side of that one-directional edge, reaching into
 * {@code catalog} only through the published {@link ProductStockReceiver}. Before this listener
 * existed, nothing ever moved a storable product's stock count on a sale: only the admin's manual
 * "Ajuster le stock" modal and supplier purchases touched it.
 */
@Component
public class ProductStockDeductionListener {

    private final ProductStockReceiver productStockReceiver;

    public ProductStockDeductionListener(ProductStockReceiver productStockReceiver) {
        this.productStockReceiver = productStockReceiver;
    }

    // Same reasoning as StockDeductionListener: @ApplicationModuleListener already runs after the
    // publishing transaction commits, so this needs its own fresh transaction to be atomic.
    @ApplicationModuleListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(OrderStatusChangedEvent event) {
        if (!"paid".equals(event.newStatus())) {
            return;
        }
        for (OrderStatusChangedEvent.SoldItem soldItem : event.items()) {
            productStockReceiver.deductForSale(soldItem.productId(), soldItem.qty(), event.orderId(), event.changedByUserId());
        }
    }
}
