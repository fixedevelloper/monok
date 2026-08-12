package com.monokek.crm.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateCustomerRequest(String name, @NotBlank String phone, @Email String email) {
}
