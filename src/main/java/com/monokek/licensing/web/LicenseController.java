package com.monokek.licensing.web;

import com.monokek.licensing.application.LicenseService;
import com.monokek.licensing.web.dto.ActivateLicenseRequest;
import com.monokek.licensing.web.dto.LicenseStatusDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Both routes are already in {@code SecurityConfig}'s public allow-list —
 * they must be reachable before a user is authenticated, since the frontend's
 * {@code LicenseGuard} wraps the whole app above the login screen.
 */
@RestController
@RequestMapping("/api/license")
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @GetMapping("/status")
    public LicenseStatusDto status() {
        return licenseService.status();
    }

    @PostMapping("/activate")
    public LicenseStatusDto activate(@Valid @RequestBody ActivateLicenseRequest request) {
        return licenseService.activate(request.key());
    }
}
