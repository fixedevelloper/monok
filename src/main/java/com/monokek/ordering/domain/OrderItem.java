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
        item.status = "pending";
        item.recalculateTotal();
        return item;
    }

    /** {@code price * qty}, plus every modifier's own {@code price * quantity} — modifiers are
     * added AFTER the item itself (see {@code OrderService#addModifiers}, called once the item
     * exists), so {@code total} can never be a one-shot value set at construction: it has to be
     * recomputed every time either side changes. Previously only {@code price * qty} — a
     * modifier's price was durably stored on {@code OrderItemModifier} (and shown correctly on
     * the printed ticket, which reads modifiers separately) but never actually made it into the
     * billed total (this field, which {@code Order#refreshTotals}/{@code OrderRound#totalRound}/
     * order history all sum) — a real undercharge, not just a display gap. */
    public void recalculateTotal() {
        BigDecimal modifiersTotal = modifiers.stream()
                .map(m -> m.getPrice().multiply(BigDecimal.valueOf(m.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.total = price.multiply(BigDecimal.valueOf(qty)).add(modifiersTotal);
    }

    public OrderItemModifier addModifier(Long modifierItemId, BigDecimal price, int quantity) {
        OrderItemModifier modifier = OrderItemModifier.of(this, modifierItemId, price, quantity);
        modifiers.add(modifier);
        recalculateTotal();
        return modifier;
    }

    /** Port of the qty-update branch of {@code OrderController::updateRoundItemQty}. */
    public void updateQty(int newQty) {
        this.qty = newQty;
        recalculateTotal();
    }
}
