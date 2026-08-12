package com.monokek.identity.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monokek.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

/**
 * Replaces Laravel's {@code auth:sanctum} + {@code role:admin|manager}
 * middleware stack (see {@code routes/api.php} and {@code bootstrap/app.php}).
 * Sessions/CSRF are irrelevant here: every client (POS, kitchen display,
 * mobile) authenticates with a bearer JWT, so the API is fully stateless.
 *
 * <p>{@code @EnableMethodSecurity} turns on {@code @PreAuthorize} as a second,
 * method-level layer on top of the path-based rules below — belt and
 * suspenders, not a replacement: every controller under {@code /api/admin/**}
 * is also annotated directly, so a future path-matching mistake here (a
 * renamed route, a copy-pasted mapping) doesn't silently drop the role
 * check the way {@code CashController#storeRegister} once did (flagged in
 * the security audit — it only had the path rule, and that rule was
 * missing).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    /**
     * Comma-separated allow-list, overridable via the {@code CORS_ALLOWED_ORIGINS} env var.
     * Defaults cover every origin this app legitimately runs from: the Next.js dev server,
     * a Tauri desktop build's webview (a fixed {@code tauri://}/{@code http://tauri.localhost}
     * origin regardless of which backend IP the user points it at in Settings — see
     * {@code src/lib/axios.ts} on the frontend), and a plain browser hitting a built static
     * export on port 80/443. {@code allowedOriginPatterns("*")} + {@code allowCredentials(true)}
     * (the previous config) let the wildcard-pattern loophole bypass the same-origin protection
     * credentialed CORS is supposed to provide — flagged in the security audit.
     */
    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Verifies/produces $2a$/$2b$/$2y$ bcrypt hashes interchangeably, so
        // it stays compatible with hashes created by Laravel's Hash::make().
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(AppUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // No anonymous principal: a request with no/invalid/expired token then has no
                // Authentication at all, so authorizeHttpRequests raises an AuthenticationException
                // (→ 401 via the entry point below) instead of an AccessDeniedException (→ 403) —
                // see the two handlers just below for why that distinction matters here.
                .anonymous(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(this::writeUnauthenticated)
                        .accessDeniedHandler(this::writeForbidden))
                .authorizeHttpRequests(auth -> auth
                        // --- ROUTES PUBLIQUES (see routes/api.php) ---
                        .requestMatchers(
                                "/api/login",
                                "/api/ping",
                                "/api/license/activate",
                                "/api/license/status",
                                "/api/print-queue/*/dispatch-network",
                                "/api/print-queue/pending",
                                "/api/print-queue/*/mark-success",
                                "/api/print-queue/*/mark-failed",
                                // Ticket-authorized, not JWT-header-authorized — native EventSource can't
                                // send an Authorization header. See com.monokek.notifications's package-info.
                                "/api/sse/stream"
                        ).permitAll()
                        // Laravel registered this one specific route (SettingsController::index) outside its
                        // role:admin|manager group, despite the /admin path — any authenticated user can read
                        // settings, only POST (write) requires the admin/manager role below.
                        .requestMatchers(HttpMethod.GET, "/api/admin/settings").authenticated()
                        // Till configuration, not a day-to-day POS action — see CashController#storeRegister's
                        // own javadoc. Also enforced by @PreAuthorize on the method itself; kept here too so the
                        // gateway-level rule matches every other piece of branch/printer/workstation config.
                        .requestMatchers(HttpMethod.POST, "/api/cash/registers").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/cash/registers/*").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/cash/registers/*").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/cash/registers/*").hasAnyRole("ADMIN", "MANAGER")
                        // --- ESPACE ADMIN (role:admin|manager) ---
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "MANAGER")
                        // --- Everything else requires a valid bearer token ---
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * No token, an unparseable one, or an expired one — {@code JwtAuthenticationFilter} swallows the
     * parse failure and simply never sets an Authentication, which without a custom entry point falls
     * through to Spring Security's default {@code Http403ForbiddenEntryPoint} (403 regardless of cause,
     * even for "not authenticated at all" — the actual bug behind the security audit's "redirection sur
     * 401 désactivée" finding: the frontend's 401 handler had nothing to ever fire on). Writes the same
     * {@code ApiResponse} envelope {@link com.monokek.common.GlobalExceptionHandler} uses everywhere
     * else, so {@code src/lib/errors.ts} on the frontend reads a message here exactly like any other
     * error response.
     */
    private void writeUnauthenticated(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
        writeJson(response, HttpStatus.UNAUTHORIZED, "Authentification requise");
    }

    /** Authenticated, but the token's role/authority isn't enough — a real 403, distinct from the case above. */
    private void writeForbidden(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException {
        writeJson(response, HttpStatus.FORBIDDEN, "Action non autorisée");
    }

    private void writeJson(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        // Bypassing Spring's HttpMessageConverter (this runs in the security filter chain, before the
        // DispatcherServlet) means nothing sets the charset for us — without this, accented characters
        // in the message ("autorisée") come out mojibake'd under the servlet container's default encoding.
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(message)));
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
