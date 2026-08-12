package com.monokek.company.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateCompanyRequest(@NotBlank String name, String phone, @Email String email) {
}
