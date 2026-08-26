package com.monokek.crm;

import java.math.BigDecimal;

/**
 * Published interface: {@code ordering} needs to price a coupon against a specific order's
 * subtotal (expiry, minimum-amount and usage-cap checks all depend on that context, unlike the
 * generic admin-facing {@code CouponController#validate}) and to record a redemption once an
 * order carrying a coupon is actually paid — without reaching into {@code crm.domain}.
 */
public interface CouponCatalog {

    /** Prices a coupon code against one order's current subtotal. {@code discountAmount} is
     * already clamped to {@code orderSubtotal} (never a bigger discount than the order itself) and
     * is {@code ZERO} whenever {@code valid} is false. */
    CouponQuote quote(String code, BigDecimal orderSubtotal);

    /** Marks one redemption — called once, when the order carrying this coupon is paid, never at
     * "apply to order" time (an order that applies a coupon then gets cancelled must not count). */
    void redeem(Long couponId);

    record CouponQuote(boolean valid, Long couponId, String code, BigDecimal discountAmount, String message) {
    }
}
