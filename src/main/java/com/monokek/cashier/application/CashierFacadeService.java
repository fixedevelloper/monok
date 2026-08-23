package com.monokek.cashier.application;

import com.monokek.cashier.CashierFacade;
import com.monokek.cashier.domain.CashSession;
import com.monokek.cashier.domain.CashSessionRepository;
import com.monokek.cashier.domain.Payment;
import com.monokek.cashier.domain.PaymentMethod;
import com.monokek.cashier.domain.PaymentMethodRepository;
import com.monokek.cashier.domain.PaymentRepository;
import com.monokek.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
class CashierFacadeService implements CashierFacade {

    private final CashSessionRepository cashSessionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentRepository paymentRepository;

    CashierFacadeService(
            CashSessionRepository cashSessionRepository,
            PaymentMethodRepository paymentMethodRepository,
            PaymentRepository paymentRepository) {
        this.cashSessionRepository = cashSessionRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findOpenSessionId(Long userId) {
        return cashSessionRepository.findFirstByUserIdAndClosedAtIsNull(userId).map(CashSession::getId);
    }

    @Override
    @Transactional
    public PaymentResult recordPayment(
            Long orderId, Long cashSessionId, String paymentMethodName, BigDecimal amount, BigDecimal amountReceived) {
        CashSession session = cashSessionRepository.findById(cashSessionId)
                .orElseThrow(() -> ApiException.badRequest("Session de caisse introuvable"));
        PaymentMethod method = paymentMethodRepository.findAll().stream()
                .filter(m -> m.getName().equalsIgnoreCase(paymentMethodName))
                .findFirst()
                .orElseThrow(() -> ApiException.badRequest("Moyen de paiement inconnu: " + paymentMethodName));

        BigDecimal changeDue = amountReceived.subtract(amount);

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setCashSession(session);
        payment.setPaymentMethod(method);
        payment.setAmount(amount);
        payment.setAmountReceived(amountReceived);
        payment.setChangeDue(changeDue);
        payment = paymentRepository.save(payment);

        return new PaymentResult(payment.getId(), changeDue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> orderIdsPaidInSession(Long cashSessionId) {
        return paymentRepository.findByCashSessionId(cashSessionId).stream().map(Payment::getOrderId).distinct().toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentSnapshot> findLatestPaymentForOrder(Long orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .max(java.util.Comparator.comparing(Payment::getId))
                .map(p -> new PaymentSnapshot(p.getPaymentMethod().getName(), p.getAmountReceived(), p.getChangeDue(), p.getCashSession().getUserId()));
    }
}
