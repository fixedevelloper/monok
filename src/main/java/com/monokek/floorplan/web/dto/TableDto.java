package com.monokek.floorplan.web.dto;

/** Mirrors {@code App\Http\Resources\RestaurantTableResource} — minus the {@code total} field; see the module's package-info.
 * {@code virtual} is new: true for a branch's always-free fallback table (see {@code ordering.OrderService}). */
public record TableDto(Long id, Long floorId, String floorName, String name, int seats, String status, boolean virtual) {
}
