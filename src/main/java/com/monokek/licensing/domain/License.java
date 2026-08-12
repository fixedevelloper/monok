package com.monokek.licensing.domain;

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

@Entity
@Table(name = "licenses")
@Getter
@Setter
@NoArgsConstructor
public class License extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The signed JWT itself — its own {@code exp} claim is the source of truth for expiry. */
    @Column(name = "token", nullable = false, columnDefinition = "TEXT")
    private String token;

    @Column(name = "activated_at", nullable = false)
    private LocalDateTime activatedAt;
}
