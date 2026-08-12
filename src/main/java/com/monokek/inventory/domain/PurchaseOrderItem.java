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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

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
}
