package com.monokek.ordering.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ApplyCouponRequest(@NotBlank String code) {
}
