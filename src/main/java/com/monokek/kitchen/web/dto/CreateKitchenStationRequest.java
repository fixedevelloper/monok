package com.monokek.kitchen.web.dto;

import com.monokek.kitchen.domain.StationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateKitchenStationRequest(@NotNull Long branchId, @NotBlank String name, @NotNull StationType type) {
}
