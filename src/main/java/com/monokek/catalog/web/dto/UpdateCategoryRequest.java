package com.monokek.catalog.web.dto;

/** All fields optional — only the ones present are applied. */
public record UpdateCategoryRequest(String name, String description, String icon, Long kitchenStationId, Boolean active) {
}
