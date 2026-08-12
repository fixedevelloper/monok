package com.monokek.licensing.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.Optional;

@NoRepositoryBean
public interface LicenseRepository extends Repository<License, Long> {

    License save(License license);

    /** The most recently activated key — re-activating keeps prior rows as an audit trail. */
    Optional<License> findTopByOrderByIdDesc();
}
