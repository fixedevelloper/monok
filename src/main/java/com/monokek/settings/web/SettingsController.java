package com.monokek.settings.web;

import com.monokek.settings.application.SettingsService;
import com.monokek.settings.web.dto.UpdateSettingsRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Port of {@code App\Http\Controllers\Api\Admin\SettingsController}.
 * {@code GET} is intentionally reachable by any authenticated user, not
 * just {@code ROLE_ADMIN}/{@code ROLE_MANAGER} — see {@code SecurityConfig}
 * for why: despite the {@code /admin} path, Laravel registered this
 * specific route outside its {@code role:admin|manager} group.
 */
@RestController
@RequestMapping("/api/admin/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public Map<String, Object> index() {
        return settingsService.index();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') and (hasRole('ADMIN') or hasAuthority('manage_settings'))")
    public Map<String, Object> update(@Valid @RequestBody UpdateSettingsRequest request) {
        Map<String, Object> data = settingsService.update(request.settings());
        return Map.of("message", "Configurations enregistrées", "data", data);
    }
}
