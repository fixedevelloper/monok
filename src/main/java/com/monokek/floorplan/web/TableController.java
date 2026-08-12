package com.monokek.floorplan.web;

import com.monokek.floorplan.application.RestaurantTableService;
import com.monokek.floorplan.web.dto.CreateTableRequest;
import com.monokek.floorplan.web.dto.TableDto;
import com.monokek.floorplan.web.dto.UpdateTableRequest;
import com.monokek.floorplan.web.dto.UpdateTableStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Port of the table-related methods of {@code App\Http\Controllers\Api\Pos\TableController}
 * and {@code Route::apiResource('tables', TableController::class)}. Full
 * admin CRUD lives under {@code /api/admin/tables} (role-gated); the
 * unrestricted POS routes are the read-only list and the status-only
 * update, matching what Laravel exposed outside the admin group.
 *
 * <p>Laravel registered both {@code apiResource}'s {@code PATCH /admin/tables/{id}}
 * (→ {@code update}) and an explicit {@code Route::patch('tables/{table}', 'updateStatus')}
 * on the exact same path — the second one silently wins for every PATCH
 * request, making {@code update} unreachable via PATCH. Resolved here by
 * giving them different verbs: {@code PUT} for a full edit, {@code PATCH}
 * for status-only.
 */
@RestController
public class TableController {

    private final RestaurantTableService tableService;

    public TableController(RestaurantTableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping("/api/pos/tables")
    public List<TableDto> posIndex() {
        return tableService.list(null);
    }

    @PatchMapping("/api/pos/tables/{id}/status")
    public TableDto updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateTableStatusRequest request) {
        return tableService.updateStatus(id, request.status());
    }

    @GetMapping("/api/admin/tables")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<TableDto> index(@RequestParam(required = false) Long floorId) {
        return tableService.list(floorId);
    }

    @PostMapping("/api/admin/tables")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public TableDto store(@Valid @RequestBody CreateTableRequest request) {
        return tableService.create(request);
    }

    @GetMapping("/api/admin/tables/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public TableDto show(@PathVariable Long id) {
        return tableService.show(id);
    }

    @PutMapping("/api/admin/tables/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public TableDto update(@PathVariable Long id, @RequestBody UpdateTableRequest request) {
        return tableService.update(id, request);
    }

    @DeleteMapping("/api/admin/tables/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void destroy(@PathVariable Long id) {
        tableService.delete(id);
    }
}
