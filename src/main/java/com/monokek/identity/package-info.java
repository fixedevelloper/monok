/**
 * Identity &amp; access module: users, devices, roles and permissions
 * (replaces Laravel Sanctum tokens + spatie/laravel-permission), plus staff
 * administration. Reference module for the DDD layering used across the
 * codebase:
 * <ul>
 *   <li>{@code domain} — the {@code User} aggregate root (raises domain
 *       events via {@link org.springframework.data.domain.DomainEvents}),
 *       {@code Role}/{@code Permission}/{@code Device}, and repository
 *       ports; {@code domain.event} is the module's {@code @NamedInterface}
 *       for cross-module event consumption (see e.g.
 *       {@code com.monokek.settings.application.ActivityLogListener})</li>
 *   <li>{@code application} — use-case orchestration ({@code AuthService},
 *       {@code StaffService}): loads/saves aggregates, publishes events that
 *       don't originate from a {@code save()} call</li>
 *   <li>{@code infrastructure} — JWT/Spring Security adapters and the JPA
 *       repository implementations behind the domain ports</li>
 *   <li>{@code web} — REST controllers + DTOs, the module's primary
 *       (inbound) adapter</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(displayName = "Identity")
package com.monokek.identity;
