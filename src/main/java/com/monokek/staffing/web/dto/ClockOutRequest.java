package com.monokek.staffing.web.dto;

import jakarta.validation.constraints.NotNull;

public record ClockOutRequest(@NotNull Long userId) {
}
