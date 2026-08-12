package com.monokek.company.web.dto;

public record WorkstationDto(Long id, Long branchId, String branchName, String name, String type, String ip) {
}
