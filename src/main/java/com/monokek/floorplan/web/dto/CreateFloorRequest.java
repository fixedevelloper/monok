package com.monokek.floorplan.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * {@code branchId} is required here, unlike Laravel's {@code storeFloor}
 * (which only validated {@code name} and auto-assigned the first branch in
 * the system via a model {@code boot()} hook — a stopgap for when there was
 * no real way to create a branch). Now that {@code company} has real branch
 * management, the caller picks the branch explicitly.
 */
public record CreateFloorRequest(@NotNull Long branchId, @NotBlank String name) {
}
