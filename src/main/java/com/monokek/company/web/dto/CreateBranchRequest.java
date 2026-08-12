package com.monokek.company.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBranchRequest(@NotNull Long companyId, @NotBlank String name, String address, String phone) {
}
