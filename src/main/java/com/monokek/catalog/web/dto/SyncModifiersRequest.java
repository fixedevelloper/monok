package com.monokek.catalog.web.dto;

import java.util.List;

public record SyncModifiersRequest(List<Long> modifierIds) {
}
