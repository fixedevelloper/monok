package com.monokek.identity.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mirrors {@code Spatie\Permission\Models\Permission} as actually used by
 * {@code StaffController} (table {@code permissions}).
 */
@Entity
@Table(name = "permissions", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "guard_name"}))
@Getter
@Setter
@NoArgsConstructor
public class Permission extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "guard_name", nullable = false)
    private String guardName = "api";

    public Permission(String name) {
        this.name = name;
    }
}
