package com.monokek.catalog.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "modifier_items")
@Getter
@NoArgsConstructor
public class ModifierItem extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modifier_id", nullable = false)
    private Modifier modifier;

    private String name;

    @Column(precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    static ModifierItem of(Modifier modifier, String name, BigDecimal price) {
        ModifierItem item = new ModifierItem();
        item.modifier = modifier;
        item.name = name;
        item.price = price;
        return item;
    }
}
