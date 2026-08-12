package com.monokek.company.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workstations")
@Getter
@Setter
@NoArgsConstructor
public class Workstation extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    private String name; // caisse1, cuisine1...

    /** pos, kitchen, admin */
    private String type;

    private String ip;
}
