package com.monokek.catalog.web.dto;

public record CategoryDto(
        Long id, Long branchId, Long kitchenStationId, String name, String slug, String description, String icon, boolean active) {
}
