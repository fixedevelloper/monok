package com.monokek.identity.application;

import com.monokek.common.ApiException;
import com.monokek.identity.domain.Role;
import com.monokek.identity.domain.User;
import com.monokek.identity.domain.UserRepository;
import com.monokek.identity.domain.event.UserLoggedInEvent;
import com.monokek.identity.infrastructure.security.JwtService;
import com.monokek.identity.web.dto.LoginRequest;
import com.monokek.identity.web.dto.LoginResponse;
import com.monokek.identity.web.dto.UserSummary;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service: port of {@code App\Http\Controllers\Api\Auth\AuthController}.
 * Sanctum's per-device token table is replaced by a stateless JWT: there is
 * nothing to revoke server-side on "logout", the client simply discards the
 * token.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher events;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ApplicationEventPublisher events) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.events = events;
    }

    /**
     * Login doesn't mutate the {@code User} aggregate, so there is no
     * {@code save()} call to piggy-back a domain event on (see
     * {@link User#domainEvents()}) — {@link UserLoggedInEvent} is published
     * directly instead. Spring Modulith still routes it reliably to any
     * {@code @ApplicationModuleListener} because publication is tracked from
     * the moment {@link ApplicationEventPublisher#publishEvent} is called
     * inside this transaction, regardless of how the event originated.
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Les identifiants sont incorrects."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Les identifiants sont incorrects.");
        }

        String role = user.getRoles().stream().findFirst().map(Role::getName).orElse("waiter");
        String token = jwtService.generateToken(
                user.getId(),
                user.getUuid(),
                user.getName(),
                user.getRoles().stream().map(Role::getName).toList(),
                user.getAllPermissionNames().stream().toList()
        );

        events.publishEvent(new UserLoggedInEvent(user.getId(), user.getUuid(), request.deviceName()));

        return new LoginResponse(new UserSummary(user.getId(), user.getName(), role), token);
    }

    @Transactional(readOnly = true)
    public void verifyPin(User user, String pin) {
        if (user.getPinCode() == null || !passwordEncoder.matches(pin, user.getPinCode())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Code PIN invalide");
        }
    }

    @Transactional
    public void updatePin(User user, String pin) {
        user.setPinCode(passwordEncoder.encode(pin));
        userRepository.save(user);
    }

    @Transactional
    public void updatePassword(User user, String oldPassword, String newPassword, String confirmation) {
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw ApiException.badRequest("L'ancien mot de passe est incorrect.");
        }
        if (!newPassword.equals(confirmation)) {
            throw ApiException.badRequest("La confirmation du mot de passe ne correspond pas.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
