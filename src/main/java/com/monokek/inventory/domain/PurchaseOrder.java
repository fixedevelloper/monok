package com.monokek.inventory.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
@Getter
@NoArgsConstructor
public class PurchaseOrder extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    private String status = "draft";

    @Column(precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PurchaseOrderItem> items = new ArrayList<>();

    /**
     * Port of {@code PurchaseOrderController::store}, which creates the PO
     * already {@code received} ("on considère ici réception immédiate") —
     * there's no separate draft/receive workflow in the Laravel source.
     */
    public static PurchaseOrder receive(Supplier supplier) {
        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.supplier = supplier;
        purchaseOrder.status = "received";
        return purchaseOrder;
    }

    public PurchaseOrderItem addItem(Ingredient ingredient, BigDecimal qty, BigDecimal price) {
        PurchaseOrderItem item = PurchaseOrderItem.of(this, ingredient, qty, price);
        items.add(item);
        this.total = this.total.add(price.multiply(qty));
        return item;
    }

    /** Same as {@link #addItem(Ingredient, BigDecimal, BigDecimal)} for a line that buys a
     * resellable {@code catalog.Product} instead of a recipe ingredient. */
    public PurchaseOrderItem addItem(Long productId, BigDecimal qty, BigDecimal price) {
        PurchaseOrderItem item = PurchaseOrderItem.ofProduct(this, productId, qty, price);
        items.add(item);
        this.total = this.total.add(price.multiply(qty));
        return item;
    }
}
