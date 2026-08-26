package com.monokek.reporting.web.dto;

import java.util.List;

/** Hand-rolled page shape (not a real {@code org.springframework.data.domain.Page}) — this data
 * comes from raw SQL against two tables, not a Spring Data repository. */
public record StockMovementPageDto(List<StockMovementRowDto> content, long totalElements) {
}
