package com.monokek.company.web.dto;

/** All fields optional — only the ones present are applied. */
public record UpdateWorkstationRequest(Long branchId, String name, String type, String ip) {
}
