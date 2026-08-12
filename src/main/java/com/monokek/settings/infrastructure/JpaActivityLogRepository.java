package com.monokek.settings.infrastructure;

import com.monokek.settings.domain.ActivityLog;
import com.monokek.settings.domain.ActivityLogRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaActivityLogRepository extends ActivityLogRepository, JpaRepository<ActivityLog, Long> {
}
