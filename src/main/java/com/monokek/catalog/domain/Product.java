package com.monokek.catalog.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@SQLRestriction("deleted_at is null")
@Getter
@Setter
@NoArgsConstructor
public class Product extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    private String sku;
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    /** Cost the branch pays to acquire/produce one unit — used as a fallback cost source in
     * {@code reporting.ReportingService#foodCostPercent} for products with no recipe (see there
     * for why that used to leave them out of the food-cost calculation entirely). Nullable: no
     * value means no cost data, same as before this field existed. */
    @Column(name = "purchase_price", precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "incentive_amount", precision = 10, scale = 2)
    private BigDecimal incentiveAmount = BigDecimal.ZERO;

    private String image;

    /** storable, consumable, service — see README for the deferred enum-vs-string decision. */
    private String type = "storable";

    @Column(name = "stock_count")
    private int stockCount = 0;

    @Column(name = "alert_stock")
    private int alertStock = 0;

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "track_stock")
    private boolean trackStock = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
