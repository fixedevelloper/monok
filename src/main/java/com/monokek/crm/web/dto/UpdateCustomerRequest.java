package com.monokek.crm.web.dto;

/** All fields optional — only the ones present are applied. */
public record UpdateCustomerRequest(String name, String phone, String email) {
}
