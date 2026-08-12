package com.monokek.crm.infrastructure;

import com.monokek.crm.domain.Coupon;
import com.monokek.crm.domain.CouponRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaCouponRepository extends CouponRepository, JpaRepository<Coupon, Long> {
}
