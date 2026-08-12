package com.monokek.settings.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface SettingRepository extends Repository<Setting, Long> {

    Setting save(Setting setting);

    Optional<Setting> findByKey(String key);

    List<Setting> findAll();
}
