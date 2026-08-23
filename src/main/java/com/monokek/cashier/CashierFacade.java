package com.monokek.cashier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Published interface: what {@code ordering} needs from the till — is there
 * an open session for this user, record a payment against it, and which
 * orders were paid in a given session — without reaching into
 * {@code cashier.domain}.
 */
public interface CashierFacade {

    Optional<Long> findOpenSessionId(Long userId);

    PaymentResult recordPayment(
            Long orderId, Long cashSessionId, String paymentMethodName, BigDecimal amount, BigDecimal amountReceived);

    List<Long> orderIdsPaidInSession(Long cashSessionId);

    /** The payment recorded for an order — used by {@code ordering}'s reprint flow to rebuild a receipt without a new payment. */
    Optional<PaymentSnapshot> findLatestPaymentForOrder(Long orderId);

    record PaymentResult(Long paymentId, BigDecimal changeDue) {
    }

    record PaymentSnapshot(String methodName, BigDecimal amountReceived, BigDecimal changeDue, Long cashierUserId) {
    }
}
