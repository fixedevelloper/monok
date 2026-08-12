package com.monokek.floorplan.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTableStatusRequest(@NotBlank String status) {
}
