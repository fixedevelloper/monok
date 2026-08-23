package com.monokek.settings.web;

import com.monokek.settings.application.ActivityLogService;
import com.monokek.settings.web.dto.ActivityLogDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-facing audit trail — see {@code ActivityLogService} for why this didn't exist until now. */
@RestController
@RequestMapping("/api/admin/activity-logs")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    public ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public Page<ActivityLogDto> index(@PageableDefault(size = 50) Pageable pageable) {
        return activityLogService.list(pageable);
    }
}
