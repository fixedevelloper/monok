package com.monokek.settings.application;

import com.monokek.cashier.domain.event.CashSessionClosedEvent;
import com.monokek.cashier.domain.event.CashSessionOpenedEvent;
import com.monokek.crm.domain.event.LoyaltyPointsChangedEvent;
import com.monokek.identity.domain.event.StaffAccessRevokedEvent;
import com.monokek.identity.domain.event.StaffCreatedEvent;
import com.monokek.identity.domain.event.StaffPermissionsUpdatedEvent;
import com.monokek.identity.domain.event.UserLoggedInEvent;
import com.monokek.ordering.domain.event.OrderCreatedEvent;
import com.monokek.ordering.domain.event.OrderStatusChangedEvent;
import com.monokek.settings.domain.ActivityLog;
import com.monokek.settings.domain.ActivityLogRepository;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Concrete proof of the event-driven wiring: none of {@code identity},
 * {@code ordering} or {@code cashier} ever reference {@code settings}, yet
 * every meaningful change to a user, an order or a cash session ends up in
 * the audit trail. This is what the (until now unused) {@code activity_logs}
 * table was clearly meant for — Laravel's {@code ActivityLog} model existed
 * but nothing ever wrote to it.
 *
 * <p>{@code @ApplicationModuleListener} = {@code @Async} +
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} + its own
 * transaction, backed by Spring Modulith's JDBC event publication registry
 * (see {@code spring-modulith-starter-jdbc} in the POM): if this listener
 * throws or the process crashes mid-flight, the unfinished publication stays
 * in the log and is retried, instead of the event silently getting lost the
 * way an in-memory {@code ApplicationListener} would.
 */
@Component
public class ActivityLogListener {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogListener(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @ApplicationModuleListener
    void on(UserLoggedInEvent event) {
        record(event.userId(), "Connexion depuis '%s'".formatted(
                event.deviceName() == null ? "appareil inconnu" : event.deviceName()));
    }

    @ApplicationModuleListener
    void on(StaffCreatedEvent event) {
        record(event.userId(), "Membre du staff créé: %s (%s)".formatted(event.name(), event.role()));
    }

    @ApplicationModuleListener
    void on(StaffPermissionsUpdatedEvent event) {
        record(event.userId(), "Permissions mises à jour: %s".formatted(String.join(", ", event.permissions())));
    }

    @ApplicationModuleListener
    void on(StaffAccessRevokedEvent event) {
        record(event.userId(), "Accès révoqué par l'utilisateur #%d".formatted(event.revokedByUserId()));
    }

    @ApplicationModuleListener
    void on(OrderCreatedEvent event) {
        record(event.waiterUserId(), "Commande %s ouverte".formatted(event.reference()));
    }

    @ApplicationModuleListener
    void on(OrderStatusChangedEvent event) {
        record(event.changedByUserId(), "Commande #%d : %s -> %s".formatted(event.orderId(), event.previousStatus(), event.newStatus()));
    }

    @ApplicationModuleListener
    void on(CashSessionOpenedEvent event) {
        record(event.userId(), "Caisse #%d ouverte avec un fond de %s".formatted(event.registerId(), event.openingAmount()));
    }

    @ApplicationModuleListener
    void on(CashSessionClosedEvent event) {
        record(event.userId(), "Caisse fermée : compté %s, attendu %s (écart %s)".formatted(
                event.closingAmount(), event.expectedAmount(), event.difference()));
    }

    @ApplicationModuleListener
    void on(LoyaltyPointsChangedEvent event) {
        // No staff user attached to a loyalty change — activity_logs.user_id stays null, the customer is named in the action text.
        record(null, "Client #%d : %s %d points (nouveau solde: %d)".formatted(
                event.customerId(), "earn".equals(event.type()) ? "+" : "-", event.points(), event.newBalance()));
    }

    private void record(Long userId, String action) {
        ActivityLog log = new ActivityLog();
        log.setUserId(userId);
        log.setAction(action);
        activityLogRepository.save(log);
    }
}
