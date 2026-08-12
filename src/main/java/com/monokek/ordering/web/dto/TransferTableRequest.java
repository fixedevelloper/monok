package com.monokek.ordering.web.dto;

import jakarta.validation.constraints.NotNull;

public record TransferTableRequest(@NotNull Long fromTableId, @NotNull Long toTableId) {
}
