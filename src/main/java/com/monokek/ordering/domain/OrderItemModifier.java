package com.monokek.ordering.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item_modifiers")
@Getter
@NoArgsConstructor
public class OrderItemModifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    /** References catalog.ModifierItem by id only. */
    @Column(name = "modifier_item_id")
    private Long modifierItemId;

    private int quantity = 1;

    @Column(precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    /** Generated column ({@code quantity * price}) — read-only from JPA's side. */
    @Column(precision = 12, scale = 2, insertable = false, updatable = false)
    private BigDecimal total;

    static OrderItemModifier of(OrderItem item, Long modifierItemId, BigDecimal price, int quantity) {
        OrderItemModifier modifier = new OrderItemModifier();
        modifier.orderItem = item;
        modifier.modifierItemId = modifierItemId;
        modifier.price = price;
        modifier.quantity = quantity;
        return modifier;
    }
}
