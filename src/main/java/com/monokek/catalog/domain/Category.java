package com.monokek.catalog.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** References company.Branch by id only — see module package-info. */
    private Long branchId;

    /**
     * References kitchen.KitchenStation by id only. Kept as a plain id (not a
     * JPA association) so `catalog` never has to depend on `kitchen`, which
     * itself depends on `ordering`, which depends on `catalog` — a real cycle
     * in the domain that Spring Modulith would otherwise reject.
     */
    private Long kitchenStationId;

    private String name;
    private String slug;
    private String description;
    private String icon;

    @Column(name = "is_active")
    private boolean active = true;
}
