package com.monokek.catalog.web.dto;

public record ProductStockMovementDto(
        Long id, Long productId, String type, int qty, String reason,
        Long authorId, String authorName, String createdAt) {
}
