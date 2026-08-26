package com.monokek.crm.application;

import com.monokek.common.ApiException;
import com.monokek.crm.CouponCatalog;
import com.monokek.crm.domain.Coupon;
import com.monokek.crm.domain.CouponRepository;
import com.monokek.crm.domain.event.CouponPrintRequestedEvent;
import com.monokek.crm.web.dto.CouponDto;
import com.monokek.crm.web.dto.CouponValidationResult;
import com.monokek.crm.web.dto.CreateCouponRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** New functionality: coupons existed as a bare model/table, with no controller/business rules anywhere in Laravel.
 * Also implements {@link CouponCatalog} — the order-aware pricing/redemption {@code ordering} needs is the same
 * business logic as the admin-facing CRUD/validate here, not a separate concern worth its own class. */
@Service
public class CouponService implements CouponCatalog {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final CouponRepository couponRepository;
    private final ApplicationEventPublisher events;

    public CouponService(CouponRepository couponRepository, ApplicationEventPublisher events) {
        this.couponRepository = couponRepository;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<CouponDto> list() {
        return couponRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public CouponDto create(CreateCouponRequest request) {
        if (couponRepository.existsByCode(request.code())) {
            throw ApiException.conflict("Ce code coupon existe déjà.");
        }
        Coupon coupon = new Coupon();
        coupon.setCode(request.code());
        coupon.setAmount(request.amount());
        coupon.setMinAmount(request.minAmount());
        coupon.setExpiresAt(request.expiresAt());
        coupon.setMaxUses(request.maxUses());
        return toDto(couponRepository.save(coupon));
    }

    /** Checks a code is real and not expired — the schema has no "already used" tracking, so a valid coupon is reusable. */
    @Transactional(readOnly = true)
    public CouponValidationResult validate(String code) {
        return couponRepository.findByCode(code)
                .map(coupon -> coupon.isExpired()
                        ? new CouponValidationResult(false, BigDecimal.ZERO, "Ce coupon a expiré.")
                        : new CouponValidationResult(true, coupon.getAmount(), "Coupon valide."))
                .orElseGet(() -> new CouponValidationResult(false, BigDecimal.ZERO, "Code coupon inconnu."));
    }

    /** Asks the requesting branch's receipt printer to print this coupon — see {@code printing.application.PrintQueueListener}. */
    @Transactional(readOnly = true)
    public void printCoupon(Long couponId, Long branchId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> ApiException.notFound("Coupon introuvable."));
        events.publishEvent(new CouponPrintRequestedEvent(
                coupon.getId(), coupon.getCode(), coupon.getAmount(),
                coupon.getExpiresAt() == null ? null : coupon.getExpiresAt().format(DATE_TIME),
                branchId));
    }

    /** {@link CouponCatalog#quote}: unlike {@link #validate}, this is order-aware — it also checks
     * the usage cap and the minimum-order-amount rule, and clamps the discount to what the order
     * can actually absorb (never a bigger discount than the order's own subtotal). */
    @Override
    @Transactional(readOnly = true)
    public CouponQuote quote(String code, BigDecimal orderSubtotal) {
        return couponRepository.findByCode(code)
                .map(coupon -> {
                    if (coupon.isExpired()) {
                        return new CouponQuote(false, null, code, BigDecimal.ZERO, "Ce coupon a expiré.");
                    }
                    if (coupon.isExhausted()) {
                        return new CouponQuote(false, null, code, BigDecimal.ZERO, "Ce coupon a atteint son nombre maximum d'utilisations.");
                    }
                    if (!coupon.meetsMinimum(orderSubtotal)) {
                        return new CouponQuote(false, null, code, BigDecimal.ZERO,
                                "Montant minimum requis : " + coupon.getMinAmount() + " FCFA.");
                    }
                    BigDecimal discount = coupon.getAmount().min(orderSubtotal);
                    return new CouponQuote(true, coupon.getId(), coupon.getCode(), discount, "Coupon appliqué.");
                })
                .orElseGet(() -> new CouponQuote(false, null, code, BigDecimal.ZERO, "Code coupon inconnu."));
    }

    @Override
    @Transactional
    public void redeem(Long couponId) {
        couponRepository.findById(couponId).ifPresent(coupon -> {
            coupon.recordUse();
            couponRepository.save(coupon);
        });
    }

    private CouponDto toDto(Coupon coupon) {
        return new CouponDto(
                coupon.getId(), coupon.getCode(), coupon.getAmount(), coupon.getMinAmount(),
                coupon.getExpiresAt() == null ? null : coupon.getExpiresAt().format(DATE_TIME),
                coupon.getMaxUses(), coupon.getTimesUsed(), coupon.isExpired());
    }
}
