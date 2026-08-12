package com.monokek.cashier.web.dto;

/** All fields optional — only the ones present are applied. */
public record UpdateRegisterRequest(String name, Long branchId) {
}
