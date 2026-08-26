package com.monokek.inventory.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
public class StockMovement extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    /** in, out, adjust */
    private String type;

    @Column(precision = 12, scale = 3)
    private BigDecimal qty;

    @Column(columnDefinition = "TEXT")
    private String reason;

    /** Who performed the movement — null for the automatic sale-deduction listener (see {@code
     * StockDeductionListener}), which has no human actor. References identity.User by id only,
     * resolved to a display name via {@code UserDirectory} at read time — see {@code
     * catalog.domain.ProductStockMovement#authorId} for the same convention. */
    @Column(name = "author_id")
    private Long authorId;
}
