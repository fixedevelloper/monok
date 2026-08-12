package com.monokek.identity.web;

import com.monokek.common.ApiResponse;
import com.monokek.identity.application.StaffService;
import com.monokek.identity.infrastructure.security.AuthenticatedUser;
import com.monokek.identity.web.dto.StaffCreateRequest;
import com.monokek.identity.web.dto.StaffDto;
import com.monokek.identity.web.dto.StaffUpdateRequest;
import com.monokek.identity.web.dto.UpdatePermissionsRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Port of {@code App\Http\Controllers\Api\Admin\StaffController}. Guarded by
 * {@code hasAnyRole("ADMIN", "MANAGER")} in {@code SecurityConfig}, mirroring
 * the Laravel {@code role:admin|manager} middleware on {@code /admin/**}.
 */
@RestController
@RequestMapping("/api/admin/staff")
/** Defense in depth: {@code SecurityConfig} already gates {@code /api/admin/**} to ADMIN/MANAGER — this is a second, method-level check that survives even if the path-based rule is ever misconfigured. */
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    public ApiResponse<List<StaffDto>> index() {
        return ApiResponse.success(staffService.list());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StaffDto> store(@Valid @RequestBody StaffCreateRequest request) {
        return ApiResponse.success(staffService.create(request), "Membre du staff créé avec succès");
    }

    @RequestMapping(value = "/{uuid}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ApiResponse<StaffDto> update(@PathVariable UUID uuid, @RequestBody StaffUpdateRequest request) {
        return ApiResponse.success(staffService.update(uuid, request), "Profil mis à jour");
    }

    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> destroy(@PathVariable UUID uuid, @AuthenticationPrincipal AuthenticatedUser principal) {
        staffService.delete(uuid, principal.getUser().getId());
        return ApiResponse.message("Accès révoqué avec succès");
    }

    @GetMapping("/roles")
    public ApiResponse<List<StaffDto.RoleOption>> roles() {
        return ApiResponse.success(staffService.listRoles());
    }

    @GetMapping("/permissions/list")
    public ApiResponse<List<StaffDto.PermissionOption>> permissions() {
        return ApiResponse.success(staffService.listPermissions());
    }

    @PutMapping("/{uuid}/permissions")
    public ApiResponse<Integer> updatePermissions(@PathVariable UUID uuid, @Valid @RequestBody UpdatePermissionsRequest request) {
        int count = staffService.updatePermissions(uuid, request.permissions());
        return ApiResponse.success(count, "Permissions mises à jour avec succès");
    }
}
