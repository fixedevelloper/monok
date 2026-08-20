package com.monokek.crm.application;

import com.monokek.common.ApiException;
import com.monokek.crm.domain.Coupon;
import com.monokek.crm.domain.CouponRepository;
import com.monokek.crm.domain.event.CouponPrintRequestedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CouponServiceTest {

    @Test
    void publishesACouponPrintRequestedEventForTheRequestingBranch() {
        Coupon coupon = new Coupon();
        coupon.setId(5L);
        coupon.setCode("PROMO10");
        coupon.setAmount(new BigDecimal("1000"));
        coupon.setExpiresAt(LocalDateTime.of(2027, 1, 1, 0, 0));

        CouponRepository repository = mock(CouponRepository.class);
        when(repository.findById(5L)).thenReturn(Optional.of(coupon));

        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        CouponService service = new CouponService(repository, events);

        service.printCoupon(5L, 3L);

        var captor = org.mockito.ArgumentCaptor.forClass(CouponPrintRequestedEvent.class);
        verify(events).publishEvent(captor.capture());
        CouponPrintRequestedEvent published = captor.getValue();
        assertThat(published.couponId()).isEqualTo(5L);
        assertThat(published.code()).isEqualTo("PROMO10");
        assertThat(published.amount()).isEqualByComparingTo("1000");
        assertThat(published.branchId()).isEqualTo(3L);
        assertThat(published.expiresAt()).startsWith("2027-01-01");
    }

    @Test
    void rejectsPrintingAnUnknownCoupon() {
        CouponRepository repository = mock(CouponRepository.class);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        CouponService service = new CouponService(repository, events);

        assertThatThrownBy(() -> service.printCoupon(99L, 3L)).isInstanceOf(ApiException.class);
        verify(events, never()).publishEvent(any());
    }
}
