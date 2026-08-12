package com.monokek.company.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateWorkstationRequest(@NotNull Long branchId, @NotBlank String name, @NotBlank String type, String ip) {
}
