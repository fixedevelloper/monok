package com.monokek.identity.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

@NoRepositoryBean
public interface DeviceRepository extends Repository<Device, Long> {

    Device save(Device device);
}
