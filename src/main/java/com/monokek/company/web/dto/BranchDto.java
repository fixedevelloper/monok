package com.monokek.company.web.dto;

public record BranchDto(Long id, Long companyId, String companyName, String name, String address, String phone) {
}
