package com.monokek.company.web.dto;

/** All fields optional — only the ones present are applied. */
public record UpdateBranchRequest(Long companyId, String name, String address, String phone) {
}
