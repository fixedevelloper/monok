package com.monokek.staffing.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** A planned roster entry — who's expected to work when. See the module's package-info. */
@Entity
@Table(name = "shifts")
@Getter
@Setter
@NoArgsConstructor
public class Shift extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** References identity.User by id only — see module package-info. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** References company.Branch by id only — see module package-info. */
    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    private String note;
}
