package com.monokek.catalog.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Audit trail for {@link Product#getStockCount()} changes made through the admin "Ajuster le
 * stock" modal — the only path allowed to move {@code stockCount} now that
 * {@code UpdateProductRequest} no longer accepts it directly. Deliberately its own table/entity
 * rather than reusing {@code inventory.domain.StockMovement}: that one is FK'd to {@code
 * ingredient_id} (recipe raw materials), a different concept from a storable product's own
 * sellable-unit count, and reusing it would mean a cross-module JPA relation into {@code
 * catalog.domain.Product} that Spring Modulith's boundary (see {@code ModularityTests}) doesn't
 * allow.
 */
@Entity
@Table(name = "product_stock_movements")
@Getter
@Setter
@NoArgsConstructor
public class ProductStockMovement extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** in, out, adjust */
    private String type;

    /** Signed change in {@code stockCount} actually applied (negative for "out"). */
    private int qty;

    @Column(columnDefinition = "TEXT")
    private String reason;

    /** Who performed the adjustment — resolved to a display name via {@code identity.UserDirectory}
     * at read time rather than a JPA relation, same cross-module convention as {@code
     * ordering.domain.Order#userId}/{@code cashierId}. */
    @Column(name = "author_id", nullable = false)
    private Long authorId;
}
