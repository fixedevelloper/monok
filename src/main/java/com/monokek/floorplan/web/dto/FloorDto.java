package com.monokek.floorplan.web.dto;

import java.util.List;

/** Mirrors {@code App\Http\Resources\FloorResource} — minus the {@code currentOrder} total; see the module's package-info. */
public record FloorDto(Long id, Long branchId, String name, int tablesCount, List<TableDto> tables) {
}
