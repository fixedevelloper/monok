package com.monokek.settings.application;

import com.monokek.identity.UserDirectory;
import com.monokek.settings.domain.ActivityLog;
import com.monokek.settings.domain.ActivityLogRepository;
import com.monokek.settings.web.dto.ActivityLogDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Read side of the audit trail {@link ActivityLogListener} writes — until now the {@code
 * activity_logs} table was write-only, invisible to admins even though every meaningful action
 * (order cancelled/round voided, staff/permission changes, cash session open/close, ...) has been
 * logged there. Backs {@code GET /api/admin/activity-logs}.
 */
@Service
public class ActivityLogService {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ActivityLogRepository activityLogRepository;
    private final UserDirectory userDirectory;

    public ActivityLogService(ActivityLogRepository activityLogRepository, UserDirectory userDirectory) {
        this.activityLogRepository = activityLogRepository;
        this.userDirectory = userDirectory;
    }

    /** {@code startDate}/{@code endDate} optional — same conversion as {@code
     * OrderService#historyAdmin}: whole-day bounds, {@code null} left unbounded on that side. */
    @Transactional(readOnly = true)
    public Page<ActivityLogDto> list(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime from = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime to = endDate == null ? null : endDate.atTime(LocalTime.MAX);
        Page<ActivityLog> page = activityLogRepository.search(from, to, pageable);

        Set<Long> userIds = new LinkedHashSet<>();
        page.forEach(log -> {
            if (log.getUserId() != null) userIds.add(log.getUserId());
        });
        Map<Long, String> names = userDirectory.namesByIds(userIds);

        return page.map(log -> new ActivityLogDto(
                log.getId(),
                log.getUserId(),
                log.getUserId() == null ? null : names.get(log.getUserId()),
                log.getAction(),
                log.getCreatedAt() == null ? null : log.getCreatedAt().format(TIMESTAMP)));
    }
}
