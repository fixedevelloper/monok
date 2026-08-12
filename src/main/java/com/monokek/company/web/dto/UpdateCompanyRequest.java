package com.monokek.company.web.dto;

/** All fields optional — only the ones present are applied. */
public record UpdateCompanyRequest(String name, String phone, String email) {
}
