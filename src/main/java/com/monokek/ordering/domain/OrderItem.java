package com.monokek.ordering.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor
public class OrderItem extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_round_id", nullable = false)
    private OrderRound orderRound;

    /** References catalog.Product by id only — see module package-info. */
    @Column(name = "product_id")
    private Long productId;

    /** References catalog.ProductVariant by id only. */
    @Column(name = "variant_id")
    private Long variantId;

    private int qty;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(precision = 12, scale = 2)
    private BigDecimal total;

    private String status = "pending";

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItemModifier> modifiers = new ArrayList<>();

    static OrderItem of(OrderRound round, Long productId, Long variantId, int qty, BigDecimal unitPrice) {
        OrderItem item = new OrderItem();
        item.orderRound = round;
        item.productId = productId;
        item.variantId = variantId;
        item.qty = qty;
        item.price = unitPrice;
        item.total = unitPrice.multiply(BigDecimal.valueOf(qty));
        item.status = "pending";
        return item;
    }

    public OrderItemModifier addModifier(Long modifierItemId, BigDecimal price, int quantity) {
        OrderItemModifier modifier = OrderItemModifier.of(this, modifierItemId, price, quantity);
        modifiers.add(modifier);
        return modifier;
    }

    /** Port of the qty-update branch of {@code OrderController::updateRoundItemQty}. */
    public void updateQty(int newQty) {
        this.qty = newQty;
        this.total = price.multiply(BigDecimal.valueOf(newQty));
    }
}
