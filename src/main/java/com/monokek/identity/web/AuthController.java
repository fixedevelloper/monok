package com.monokek.identity.web;

import com.monokek.common.ApiResponse;
import com.monokek.identity.application.AuthService;
import com.monokek.identity.infrastructure.security.AuthenticatedUser;
import com.monokek.identity.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Port of {@code App\Http\Controllers\Api\Auth\AuthController}.
 * Base path matches Laravel's implicit {@code /api} prefix (RouteServiceProvider).
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        // Kept as a bare object (not wrapped in "data") to match AuthController::login's raw JSON shape.
        return new ApiResponse<>("success", null, response, null);
    }

    @GetMapping("/me")
    public StaffDto me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return StaffDto.from(principal.getUser());
    }

    @PostMapping("/auth/verify-pin")
    public ApiResponse<UserSummary> verifyPin(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody VerifyPinRequest request) {
        authService.verifyPin(principal.getUser(), request.pin());
        var user = principal.getUser();
        String role = user.getRoles().stream().findFirst().map(r -> r.getName()).orElse(null);
        return ApiResponse.success(new UserSummary(user.getId(), user.getName(), role), "Accès autorisé");
    }

    @PostMapping("/auth/update-pin")
    public ApiResponse<Void> updatePin(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdatePinRequest request) {
        authService.updatePin(principal.getUser(), request.pin());
        return ApiResponse.message("Code PIN mis à jour avec succès.");
    }

    @PostMapping("/auth/update-password")
    public ApiResponse<Void> updatePassword(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdatePasswordRequest request) {
        authService.updatePassword(
                principal.getUser(), request.oldPassword(), request.newPassword(), request.newPasswordConfirmation());
        return ApiResponse.message("Mot de passe mis à jour.");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // Stateless JWT: nothing to revoke server-side, the client discards the token.
        return ApiResponse.message("Déconnexion réussie");
    }
}
