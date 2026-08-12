package com.monokek.floorplan.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTableRequest(@NotNull Long floorId, @NotBlank String name, @Min(1) Integer seats) {
}
