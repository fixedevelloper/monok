package com.monokek.settings.web;

import com.monokek.identity.domain.event.StaffAccessRevokedEvent;
import com.monokek.identity.domain.event.StaffCreatedEvent;
import com.monokek.identity.domain.event.StaffPermissionsUpdatedEvent;
import com.monokek.identity.domain.event.UserLoggedInEvent;
import com.monokek.settings.application.ActivityLogListener;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Replaces the in-process Spring Modulith event publication that used to feed
 * {@link ActivityLogListener} directly (back when {@code identity}'s
 * {@code AuthService}/{@code StaffService} lived in this same process) — now that
 * user management/authentication is monokek-identity, it POSTs here instead, with
 * a client_credentials token (see SecurityConfig's {@code /internal/activity-log/**}
 * rule). The event record types are unchanged and still the sanctioned cross-module
 * contract (identity.domain.event's {@code @NamedInterface}), just carried over HTTP
 * instead of Spring's ApplicationEventPublisher.
 */
@RestController
@RequestMapping("/internal/activity-log")
public class InternalActivityLogController {

    private final ActivityLogListener activityLogListener;

    public InternalActivityLogController(ActivityLogListener activityLogListener) {
        this.activityLogListener = activityLogListener;
    }

    @PostMapping("/user-logged-in")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void userLoggedIn(@RequestBody UserLoggedInEvent event) {
        activityLogListener.on(event);
    }

    @PostMapping("/staff-created")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void staffCreated(@RequestBody StaffCreatedEvent event) {
        activityLogListener.on(event);
    }

    @PostMapping("/staff-permissions-updated")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void staffPermissionsUpdated(@RequestBody StaffPermissionsUpdatedEvent event) {
        activityLogListener.on(event);
    }

    @PostMapping("/staff-access-revoked")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void staffAccessRevoked(@RequestBody StaffAccessRevokedEvent event) {
        activityLogListener.on(event);
    }
}
