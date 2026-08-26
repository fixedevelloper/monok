package com.monokek.inventory.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_items")
@Getter
@NoArgsConstructor
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    /** Nullable now — exactly one of this and {@link #productId} is set. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    /** References catalog.Product by id only — see module package-info. Set instead of {@link
     * #ingredient} when this line is a resellable product (e.g. a crate of soda) rather than a
     * recipe raw material. */
    @Column(name = "product_id")
    private Long productId;

    @Column(precision = 12, scale = 3)
    private BigDecimal qty;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    static PurchaseOrderItem of(PurchaseOrder purchaseOrder, Ingredient ingredient, BigDecimal qty, BigDecimal price) {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.purchaseOrder = purchaseOrder;
        item.ingredient = ingredient;
        item.qty = qty;
        item.price = price;
        return item;
    }

    static PurchaseOrderItem ofProduct(PurchaseOrder purchaseOrder, Long productId, BigDecimal qty, BigDecimal price) {
        PurchaseOrderItem item = new PurchaseOrderItem();
        item.purchaseOrder = purchaseOrder;
        item.productId = productId;
        item.qty = qty;
        item.price = price;
        return item;
    }
}
