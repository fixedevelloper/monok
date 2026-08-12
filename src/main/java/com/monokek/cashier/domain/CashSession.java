package com.monokek.cashier.domain;

import com.monokek.cashier.domain.event.CashSessionClosedEvent;
import com.monokek.cashier.domain.event.CashSessionOpenedEvent;
import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Aggregate root: port of the cash-session lifecycle Laravel spreads across
 * {@code CashController::open}/{@code close}. Stages
 * {@link CashSessionOpenedEvent}/{@link CashSessionClosedEvent} via
 * {@code @DomainEvents} — see {@code identity.domain.User} for why this
 * hook is used instead of {@code AbstractAggregateRoot} (already extends
 * {@code Timestamps}).
 */
@Entity
@Table(name = "cash_sessions")
@Getter
@NoArgsConstructor
public class CashSession extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "register_id", nullable = false)
    private CashRegister register;

    /** References identity.User by id only. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "opening_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal openingAmount;

    @Column(name = "closing_amount", precision = 12, scale = 2)
    private BigDecimal closingAmount;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /** System-computed theoretical amount, filled in on close. */
    @Column(name = "expected_amount", precision = 12, scale = 2)
    private BigDecimal expectedAmount;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Transient
    private final transient List<UnresolvedEvent> pendingEvents = new ArrayList<>();

    /** Opens a new session. Stages a {@link CashSessionOpenedEvent}. */
    public static CashSession open(CashRegister register, Long userId, BigDecimal openingAmount) {
        CashSession session = new CashSession();
        session.register = register;
        session.userId = userId;
        session.openingAmount = openingAmount;
        session.openedAt = LocalDateTime.now();
        session.pendingEvents.add(new UnresolvedEvent(EventKind.OPENED, null));
        return session;
    }

    /** Closes the session with the counted amount and the system-computed expectation. Stages a {@link CashSessionClosedEvent}. */
    public void close(BigDecimal closingAmount, BigDecimal expectedAmount, String note) {
        this.closingAmount = closingAmount;
        this.expectedAmount = expectedAmount;
        this.closedAt = LocalDateTime.now();
        this.note = note;
        pendingEvents.add(new UnresolvedEvent(EventKind.CLOSED, null));
    }

    public boolean isOpen() {
        return closedAt == null;
    }

    @DomainEvents
    Collection<Object> domainEvents() {
        return pendingEvents.stream().map(this::resolve).toList();
    }

    @AfterDomainEventPublication
    void clearDomainEvents() {
        pendingEvents.clear();
    }

    private Object resolve(UnresolvedEvent pending) {
        return switch (pending.kind) {
            case OPENED -> new CashSessionOpenedEvent(id, register.getId(), userId, openingAmount);
            case CLOSED -> new CashSessionClosedEvent(id, userId, closingAmount, expectedAmount);
        };
    }

    private enum EventKind { OPENED, CLOSED }

    private record UnresolvedEvent(EventKind kind, Object payload) {
    }
}
