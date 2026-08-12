package com.monokek.settings.infrastructure;

import com.monokek.settings.domain.Setting;
import com.monokek.settings.domain.SettingRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaSettingRepository extends SettingRepository, JpaRepository<Setting, Long> {
}
