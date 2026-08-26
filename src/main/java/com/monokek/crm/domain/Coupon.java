package com.monokek.crm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    private BigDecimal amount;

    /** Minimum order subtotal required to apply this coupon. Null means no minimum. */
    @Column(name = "min_amount")
    private BigDecimal minAmount;

    private LocalDateTime expiresAt;

    /** Caps how many orders can redeem this coupon in total. Null means unlimited — the coupon
     * stays reusable until it expires, same as before this field existed. */
    @Column(name = "max_uses")
    private Integer maxUses;

    /** Incremented once per order that actually gets paid with this coupon applied — see
     * {@code ordering.application.OrderService#finalizePayment}. Applying a coupon to an order
     * that later gets cancelled does NOT count as a use. */
    @Column(name = "times_used", nullable = false)
    private int timesUsed = 0;

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isExhausted() {
        return maxUses != null && timesUsed >= maxUses;
    }

    public boolean meetsMinimum(BigDecimal orderSubtotal) {
        return minAmount == null || orderSubtotal.compareTo(minAmount) >= 0;
    }

    public void recordUse() {
        timesUsed++;
    }
}
