package com.monokek.identity.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StaffCreateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 20) String phone,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String role,
        Long branchId
) {
}
