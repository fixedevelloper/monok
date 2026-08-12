package com.monokek.licensing.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Field names match the frontend's {@code useLicense.ts} {@code LicenseStatus}
 * interface verbatim (snake_case) — unlike catalog/ordering, no separate
 * {@code mapXxxDtoToXxx} layer exists on the frontend for this endpoint, so
 * the wire format has to be exactly this shape.
 */
public record LicenseStatusDto(
        boolean active,
        boolean expired,
        @JsonProperty("license_key") String licenseKey,
        @JsonProperty("expiry_date") String expiryDate,
        @JsonProperty("days_left") long daysLeft
) {
}
