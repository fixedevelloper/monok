package com.monokek.catalog.web;

import com.monokek.catalog.application.ModifierService;
import com.monokek.catalog.web.dto.CreateModifierItemRequest;
import com.monokek.catalog.web.dto.CreateModifierRequest;
import com.monokek.catalog.web.dto.ModifierDto;
import com.monokek.catalog.web.dto.UpdateModifierRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Port of {@code App\Http\Controllers\Api\Admin\ModifierController}. Guarded
 * by {@code hasAnyRole("ADMIN", "MANAGER")} — see {@code SecurityConfig}.
 * {@code show} is a real implementation of the route Laravel's
 * {@code apiResource('modifiers', ...)} points at, even though the source
 * controller never defined the method.
 */
@RestController
/** Defense in depth: {@code SecurityConfig} already gates {@code /api/admin/**} to ADMIN/MANAGER — this is a second, method-level check that survives even if the path-based rule is ever misconfigured. */
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class ModifierController {

    private final ModifierService modifierService;

    public ModifierController(ModifierService modifierService) {
        this.modifierService = modifierService;
    }

    @GetMapping("/api/admin/modifiers")
    public List<ModifierDto> index() {
        return modifierService.list();
    }

    @GetMapping("/api/admin/modifiers/{id}")
    public ModifierDto show(@PathVariable Long id) {
        return modifierService.show(id);
    }

    @PostMapping("/api/admin/modifiers")
    @ResponseStatus(HttpStatus.CREATED)
    public ModifierDto store(@Valid @RequestBody CreateModifierRequest request) {
        return modifierService.create(request);
    }

    @PutMapping("/api/admin/modifiers/{id}")
    public ModifierDto update(@PathVariable Long id, @Valid @RequestBody UpdateModifierRequest request) {
        return modifierService.update(id, request);
    }

    @DeleteMapping("/api/admin/modifiers/{id}")
    public Map<String, String> destroy(@PathVariable Long id) {
        modifierService.delete(id);
        return Map.of("message", "Groupe supprimé avec succès");
    }

    @PostMapping("/api/admin/modifiers/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ModifierDto addItem(@PathVariable Long id, @Valid @RequestBody CreateModifierItemRequest request) {
        return modifierService.addItem(id, request);
    }

    @DeleteMapping("/api/admin/modifier-items/{itemId}")
    public Map<String, String> destroyItem(@PathVariable Long itemId) {
        modifierService.destroyItem(itemId);
        return Map.of("message", "Option supprimée");
    }
}
