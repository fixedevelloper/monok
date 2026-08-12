package com.monokek.floorplan.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateFloorRequest(@NotBlank String name) {
}
