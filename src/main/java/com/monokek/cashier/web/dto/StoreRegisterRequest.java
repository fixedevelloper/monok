package com.monokek.cashier.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StoreRegisterRequest(@NotBlank String name, @NotNull Long branchId) {
}
