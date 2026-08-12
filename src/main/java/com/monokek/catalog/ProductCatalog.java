package com.monokek.catalog;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Published interface (open host service): the narrow, read-only slice of
 * the catalog other modules — {@code ordering}, chiefly — are allowed to
 * depend on. Declared at the module's root package, which Spring Modulith
 * exposes to every other module by default (unlike {@code catalog.domain},
 * which stays internal). Mirrors what {@code OrderController::sendRound}
 * reads directly off {@code Product}/{@code Category} in Laravel: price is
 * always resolved server-side, never trusted from the client.
 */
public interface ProductCatalog {

    Optional<ProductSnapshot> findProduct(Long productId);

    Optional<VariantSnapshot> findVariant(Long variantId);

    Optional<ModifierItemSnapshot> findModifierItem(Long modifierItemId);

    record ProductSnapshot(Long id, String name, BigDecimal price, BigDecimal incentiveAmount, Long kitchenStationId) {
    }

    record VariantSnapshot(Long id, String name, BigDecimal price) {
    }

    record ModifierItemSnapshot(Long id, String name, BigDecimal price) {
    }
}
