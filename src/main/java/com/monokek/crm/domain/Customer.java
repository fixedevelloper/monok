package com.monokek.crm.domain;

import com.monokek.common.ApiException;
import com.monokek.common.Timestamps;
import com.monokek.crm.domain.event.LoyaltyPointsChangedEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Aggregate root. {@code crm} has no Laravel controller to port (see the
 * README for what was actually there — just an inline
 * {@code Customer::firstOrCreate} in {@code ReservationController}, already
 * covered by {@link CustomerRepository}/{@code CustomerDirectory}); the
 * loyalty-points behavior below is new functionality built on top of the
 * existing {@code customers}/{@code loyalty_transactions} schema, following
 * the same {@code @DomainEvents} pattern as {@code identity.domain.User}.
 */
@Entity
@Table(name = "customers")
@Getter
@NoArgsConstructor
public class Customer extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String phone;
    private String email;
    private int points = 0;

    @Transient
    private final transient List<LoyaltyPointsChangedEvent> pendingEvents = new ArrayList<>();

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /** Credits the customer's balance. Stages a {@link LoyaltyPointsChangedEvent}. */
    public void earnPoints(int points) {
        if (points <= 0) {
            throw ApiException.badRequest("Le nombre de points à créditer doit être positif.");
        }
        this.points += points;
        pendingEvents.add(new LoyaltyPointsChangedEvent(id, "earn", points, this.points));
    }

    /** Debits the customer's balance — cannot go negative. Stages a {@link LoyaltyPointsChangedEvent}. */
    public void redeemPoints(int points) {
        if (points <= 0) {
            throw ApiException.badRequest("Le nombre de points à débiter doit être positif.");
        }
        if (points > this.points) {
            throw ApiException.conflict("Solde de points insuffisant.");
        }
        this.points -= points;
        pendingEvents.add(new LoyaltyPointsChangedEvent(id, "redeem", points, this.points));
    }

    @DomainEvents
    Collection<LoyaltyPointsChangedEvent> domainEvents() {
        return pendingEvents;
    }

    @AfterDomainEventPublication
    void clearDomainEvents() {
        pendingEvents.clear();
    }
}
