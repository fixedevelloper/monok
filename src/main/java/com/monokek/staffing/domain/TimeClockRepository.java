package com.monokek.staffing.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface TimeClockRepository extends Repository<TimeClockEntry, Long> {

    TimeClockEntry save(TimeClockEntry entry);

    Optional<TimeClockEntry> findById(Long id);

    /** Double clock-in guard, and "is this employee currently on duty". */
    Optional<TimeClockEntry> findFirstByUserIdAndClockOutAtIsNull(Long userId);

    /** Who's currently clocked in at this branch. */
    List<TimeClockEntry> findByBranchIdAndClockOutAtIsNull(Long branchId);

    List<TimeClockEntry> findByUserIdAndClockInAtBetween(Long userId, LocalDateTime from, LocalDateTime to);

    List<TimeClockEntry> findByBranchIdAndClockInAtBetween(Long branchId, LocalDateTime from, LocalDateTime to);
}
