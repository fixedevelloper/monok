package com.monokek.cashier.web.dto;

/** {@code openedByUserName} is null when {@code isOpen} is false. */
public record CashRegisterDto(Long id, Long branchId, String name, boolean isOpen, String openedByUserName) {
}
