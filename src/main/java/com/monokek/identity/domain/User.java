package com.monokek.identity.domain;

import com.monokek.common.Timestamps;
import com.monokek.identity.domain.event.StaffAccessRevokedEvent;
import com.monokek.identity.domain.event.StaffCreatedEvent;
import com.monokek.identity.domain.event.StaffPermissionsUpdatedEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Aggregate root: port of {@code App\Models\User}. Roles/permissions replace
 * the spatie/laravel-permission {@code HasRoles} trait; tokens are replaced
 * by stateless JWTs (see {@code identity.infrastructure.security}) instead
 * of Sanctum's {@code personal_access_tokens} table.
 *
 * <p>State-changing operations go through the named methods below rather
 * than the Lombok setters, because each one is also responsible for staging
 * the domain event that {@link #domainEvents()} exposes to Spring Data: the
 * event is only ever published once {@code save()} actually succeeds (see
 * {@link org.springframework.data.domain.DomainEvents}), which also means it
 * is safe to reference {@code id}/{@code uuid} when building it — by the
 * time {@code save()} returns for an {@code IDENTITY} strategy, both are set.
 */
@Entity
@Table(name = "users")
@SQLRestriction("deleted_at is null")
@Getter
@NoArgsConstructor
public class User extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private UUID uuid;

    private String name;

    private String phone;

    /** References company.Branch by id only (see company's package-info) — null means
     * "no assigned branch" (e.g. an owner/super-admin), which resolves to unscoped access. */
    @Column(name = "branch_id")
    private Long branchId;

    /** Bcrypt hash of the 4-digit quick-unlock PIN (never the raw PIN). */
    @Column(name = "pin_code")
    private String pinCode;

    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "remember_token")
    private String rememberToken;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new LinkedHashSet<>();

    /** Extra permissions granted directly to the user, on top of their role(s). */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_permissions",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> directPermissions = new LinkedHashSet<>();

    @Transient
    private final transient List<UnresolvedEvent> pendingEvents = new ArrayList<>();

    @PrePersist
    void assignUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }

    /** Factory: register a new staff member. Stages a {@link StaffCreatedEvent}. */
    public static User register(String name, String email, String phone, String hashedPassword, Role role) {
        User user = new User();
        user.name = name;
        user.email = email;
        user.phone = phone;
        user.password = hashedPassword;
        user.roles.add(role);
        user.pendingEvents.add(new UnresolvedEvent(EventKind.CREATED, null));
        return user;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPinCode(String pinCode) {
        this.pinCode = pinCode;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    /** Replaces the direct permission grants. Stages a {@link StaffPermissionsUpdatedEvent}. */
    public void updateDirectPermissions(Set<Permission> permissions) {
        this.directPermissions = new LinkedHashSet<>(permissions);
        List<String> names = permissions.stream().map(Permission::getName).sorted().toList();
        pendingEvents.add(new UnresolvedEvent(EventKind.PERMISSIONS_UPDATED, names));
    }

    /** Soft-deletes the user and stages a {@link StaffAccessRevokedEvent}. */
    public void revokeAccess(Long revokedByUserId) {
        this.deletedAt = LocalDateTime.now();
        pendingEvents.add(new UnresolvedEvent(EventKind.ACCESS_REVOKED, revokedByUserId));
    }

    /** Union of role-granted and directly-granted permission names. */
    public Set<String> getAllPermissionNames() {
        Set<String> names = roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        directPermissions.forEach(p -> names.add(p.getName()));
        return names;
    }

    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(r -> r.getName().equalsIgnoreCase(roleName));
    }

    private String primaryRoleName() {
        return roles.stream().findFirst().map(Role::getName).orElse(null);
    }

    @DomainEvents
    Collection<Object> domainEvents() {
        return pendingEvents.stream().map(this::resolve).collect(Collectors.toList());
    }

    @AfterDomainEventPublication
    void clearDomainEvents() {
        pendingEvents.clear();
    }

    @SuppressWarnings("unchecked")
    private Object resolve(UnresolvedEvent pending) {
        return switch (pending.kind) {
            case CREATED -> new StaffCreatedEvent(id, uuid, name, primaryRoleName());
            case PERMISSIONS_UPDATED -> new StaffPermissionsUpdatedEvent(id, uuid, (List<String>) pending.payload);
            case ACCESS_REVOKED -> new StaffAccessRevokedEvent(id, uuid, (Long) pending.payload);
        };
    }

    private enum EventKind { CREATED, PERMISSIONS_UPDATED, ACCESS_REVOKED }

    /** Defers building the actual event record until id/uuid are known (post-persist). */
    private record UnresolvedEvent(EventKind kind, Object payload) {
    }
}
