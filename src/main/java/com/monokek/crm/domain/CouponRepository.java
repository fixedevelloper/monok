package com.monokek.crm.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface CouponRepository extends Repository<Coupon, Long> {

    Coupon save(Coupon coupon);

    Optional<Coupon> findById(Long id);

    List<Coupon> findAll();

    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);
}
