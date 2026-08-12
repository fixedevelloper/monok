package com.monokek.identity.infrastructure.security;

import com.monokek.identity.domain.User;
import com.monokek.identity.domain.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Reads the {@code Authorization: Bearer <token>} header the same way
 * Sanctum's {@code auth:sanctum} guard reads the plain-text token, except the
 * token here is a self-contained, stateless JWT.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Long userId = jwtService.extractUserId(token);
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    Optional<User> user = userRepository.findById(userId);
                    user.filter(User::isActive).ifPresent(u -> {
                        AuthenticatedUser principal = new AuthenticatedUser(u);
                        var authToken = new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    });
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                // Invalid/expired token: leave the request unauthenticated,
                // the security chain rejects it downstream (401/403).
            }
        }

        filterChain.doFilter(request, response);
    }
}
