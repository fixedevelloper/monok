package com.monokek.settings.web.dto;

public record ActivityLogDto(Long id, Long userId, String userName, String action, String createdAt) {
}
