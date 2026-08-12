package com.monokek.settings.infrastructure;

import com.monokek.settings.domain.SyncLog;
import com.monokek.settings.domain.SyncLogRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaSyncLogRepository extends SyncLogRepository, JpaRepository<SyncLog, Long> {
}
