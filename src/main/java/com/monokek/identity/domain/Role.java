package com.monokek.identity.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Mirrors {@code Spatie\Permission\Models\Role} (table {@code roles}).
 */
@Entity
@Table(name = "roles", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "guard_name"}))
@Getter
@Setter
@NoArgsConstructor
public class Role extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "guard_name", nullable = false)
    private String guardName = "api";

    // EAGER, matching User.roles: AuthenticatedUser#getAuthorities() reads this from
    // JwtAuthenticationFilter, outside any @Transactional/open Hibernate session
    // (spring.jpa.open-in-view is false) — LAZY here throws LazyInitializationException
    // on every single authenticated request.
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new LinkedHashSet<>();

    public Role(String name) {
        this.name = name;
    }
}
