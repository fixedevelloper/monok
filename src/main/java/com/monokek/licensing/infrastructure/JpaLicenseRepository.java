package com.monokek.licensing.infrastructure;

import com.monokek.licensing.domain.License;
import com.monokek.licensing.domain.LicenseRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaLicenseRepository extends LicenseRepository, JpaRepository<License, Long> {
}
