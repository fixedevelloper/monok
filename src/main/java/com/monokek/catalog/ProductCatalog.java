package com.monokek.catalog;

import java.math.BigDecimal;
import java.util.List;
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

    /** {@code branchId} is the product's category's branch — null means shared across every branch.
     * {@code modifierGroups} is every modifier group attached to this product, carrying the
     * selection rules {@code ordering.OrderService} enforces server-side when an item is added to
     * a round — never trusted from the client any more than price is. */
    record ProductSnapshot(
            Long id, String name, BigDecimal price, BigDecimal incentiveAmount, Long kitchenStationId, Long branchId,
            List<ModifierGroupSnapshot> modifierGroups) {
    }

    record VariantSnapshot(Long id, String name, BigDecimal price) {
    }

    /** {@code modifierGroupId} lets a caller holding several {@code ModifierItemSnapshot}s (e.g. one
     * per line of a submitted order item) tally how many were picked from each group, to check
     * against that group's own {@code ModifierGroupSnapshot} rule. */
    record ModifierItemSnapshot(Long id, String name, BigDecimal price, Long modifierGroupId) {
    }

    /** Mirrors {@code catalog.domain.Modifier}'s selection rule — see its own doc for why this
     * replaced an implicit "first group = included accompaniment" convention the POS used to infer
     * on its own. {@code type} is "accompaniment"/"supplement", purely descriptive here (the rule
     * enforced is {@code required}/{@code minSelect}/{@code maxSelect}, not the type string itself). */
    record ModifierGroupSnapshot(Long id, String name, String type, boolean required, int minSelect, Integer maxSelect) {
    }
}
