package com.monokek.inventory.web.dto;

/** All fields optional — only the ones present are applied. */
public record UpdateSupplierRequest(String name, String phone, String email, String address, String contactName) {
}
