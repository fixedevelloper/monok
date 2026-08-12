package com.monokek.identity.infrastructure.persistence;

import com.monokek.identity.domain.Device;
import com.monokek.identity.domain.DeviceRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaDeviceRepository extends DeviceRepository, JpaRepository<Device, Long> {
}
