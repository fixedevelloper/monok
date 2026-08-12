package com.monokek.kitchen.web.dto;

import com.monokek.kitchen.domain.StationType;

/** All fields optional — only the ones present are applied. */
public record UpdateKitchenStationRequest(String name, StationType type) {
}
