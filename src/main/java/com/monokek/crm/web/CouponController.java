package com.monokek.crm.web;

import com.monokek.crm.application.CouponService;
import com.monokek.crm.web.dto.CouponDto;
import com.monokek.crm.web.dto.CouponValidationResult;
import com.monokek.crm.web.dto.CreateCouponRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * New functionality (see {@code CouponService}'s javadoc). Guarded by
 * {@code hasAnyRole("ADMIN", "MANAGER")} — see {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/admin/coupons")
/** Defense in depth: {@code SecurityConfig} already gates {@code /api/admin/**} to ADMIN/MANAGER — this is a second, method-level check that survives even if the path-based rule is ever misconfigured. */
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping
    public List<CouponDto> index() {
        return couponService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponDto store(@Valid @RequestBody CreateCouponRequest request) {
        return couponService.create(request);
    }

    @GetMapping("/validate")
    public CouponValidationResult validate(@RequestParam String code) {
        return couponService.validate(code);
    }
}
