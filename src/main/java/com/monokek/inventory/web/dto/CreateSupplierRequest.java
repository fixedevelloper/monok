package com.monokek.inventory.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateSupplierRequest(@NotBlank String name, String phone, @Email String email, String address, String contactName) {
}
