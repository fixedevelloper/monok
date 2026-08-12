package com.monokek.catalog.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Join entity: which modifier (option group) groups apply to which product — port of the {@code modifier_product} pivot Laravel syncs via {@code $product->modifiers()->sync(...)}. */
@Entity
@Table(name = "modifier_product", uniqueConstraints = @UniqueConstraint(columnNames = {"modifier_id", "product_id"}))
@Getter
@NoArgsConstructor
public class ModifierProduct extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modifier_id", nullable = false)
    private Modifier modifier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "sort_order")
    private int sortOrder = 0;

    public static ModifierProduct of(Product product, Modifier modifier, int sortOrder) {
        ModifierProduct link = new ModifierProduct();
        link.product = product;
        link.modifier = modifier;
        link.sortOrder = sortOrder;
        return link;
    }
}
