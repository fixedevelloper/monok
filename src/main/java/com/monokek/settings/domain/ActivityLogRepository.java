package com.monokek.settings.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

@NoRepositoryBean
public interface ActivityLogRepository extends Repository<ActivityLog, Long> {

    ActivityLog save(ActivityLog activityLog);

    /** Backs the admin-facing audit trail — {@code settings.application.ActivityLogService}.
     * {@code from}/{@code to} optional, same null-means-unbounded convention as
     * {@code ordering.domain.OrderRepository#search}. */
    @Query("""
            SELECT a FROM ActivityLog a
            WHERE (:from IS NULL OR a.createdAt >= :from)
              AND (:to IS NULL OR a.createdAt <= :to)
            ORDER BY a.createdAt DESC
            """)
    Page<ActivityLog> search(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);
}
