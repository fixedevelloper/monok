package com.monokek.settings.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/** Mirrors the {@code {"settings": {"key1": "val1", ...}}} body {@code SettingsController::update} expects. */
public record UpdateSettingsRequest(@NotNull Map<String, Object> settings) {
}
