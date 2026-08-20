package com.monokek.printing.application.dto;

import java.math.BigDecimal;

/** {@code PrintQueue.content}, deserialized — everything {@link com.monokek.printing.application.EscPosTicketRenderer} needs for a promotional coupon. */
public record CouponContent(
        String storeName,
        String storeAddress,
        String storePhone,
        String code,
        BigDecimal amount,
        String expiresAt) {
}
