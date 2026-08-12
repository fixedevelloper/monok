package com.monokek.floorplan.web.dto;

import jakarta.validation.constraints.Min;

/**
 * All fields optional — only the ones present are applied. Laravel's
 * {@code update()} took {@code $request->all()} with no validation at all
 * (a mass-assignment gap); this replaces that with a real partial update.
 */
public record UpdateTableRequest(Long floorId, String name, @Min(1) Integer seats) {
}
