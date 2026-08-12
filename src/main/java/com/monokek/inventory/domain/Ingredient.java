package com.monokek.inventory.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "ingredients")
@Getter
@Setter
@NoArgsConstructor
public class Ingredient extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    private String name;

    @Column(precision = 12, scale = 3)
    private BigDecimal stock = BigDecimal.ZERO;

    @Column(name = "alert_qty", precision = 12, scale = 3)
    private BigDecimal alertQty = BigDecimal.ZERO;

    /**
     * Applies a stock movement — port of the {@code increment}/{@code decrement}
     * calls scattered across {@code IngredientController::adjustStock},
     * {@code PurchaseOrderController::store} and the never-wired-up
     * {@code StockService::deductFromOrder} (see {@code inventory.application.StockDeductionListener}).
     */
    public void applyMovement(String type, BigDecimal qty) {
        this.stock = switch (type) {
            case "in" -> stock.add(qty);
            case "out" -> stock.subtract(qty);
            default -> throw new IllegalArgumentException("Type de mouvement inconnu : " + type);
        };
    }

    public boolean isLowStock() {
        return stock.compareTo(alertQty) <= 0;
    }
}
