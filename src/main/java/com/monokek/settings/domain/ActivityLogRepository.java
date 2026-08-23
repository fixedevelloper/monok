package com.monokek.settings.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

@NoRepositoryBean
public interface ActivityLogRepository extends Repository<ActivityLog, Long> {

    ActivityLog save(ActivityLog activityLog);

    /** Backs the admin-facing audit trail — {@code settings.application.ActivityLogService}. */
    Page<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
