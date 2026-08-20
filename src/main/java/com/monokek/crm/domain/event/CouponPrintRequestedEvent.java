package com.monokek.crm.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Raised by {@code CouponService#printCoupon} — a coupon has no branch of its
 * own (it's a global promo code), so the branch comes from whichever POS
 * terminal asked to print it. Consumed by {@code printing.application.PrintQueueListener},
 * same shape as {@code ordering}'s {@code KitchenTicketRequestedEvent}/{@code OrderPaidEvent}.
 */
public record CouponPrintRequestedEvent(Long couponId, String code, BigDecimal amount, String expiresAt, Long branchId, Instant occurredAt) {

    public CouponPrintRequestedEvent(Long couponId, String code, BigDecimal amount, String expiresAt, Long branchId) {
        this(couponId, code, amount, expiresAt, branchId, Instant.now());
    }
}
