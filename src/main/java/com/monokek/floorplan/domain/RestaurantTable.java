package com.monokek.floorplan.domain;

import com.monokek.common.ApiException;
import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Table(name = "restaurant_tables")
@Getter
@NoArgsConstructor
public class RestaurantTable extends Timestamps {

    /**
     * Port of {@code TableController::updateStatus}'s validation list — with
     * {@code available} swapped for {@code free}. Laravel's own seed data
     * (`TableSeeder`) and the entity's default both use {@code free}, and
     * {@code ordering.TableDirectory} already writes {@code free}/{@code occupied}
     * when a round is sent or an order is paid; validating against
     * {@code available} instead (what the Laravel controller actually wrote)
     * would mean a table could never be set back to its own default/seeded
     * status through this endpoint. One vocabulary, not two.
     */
    private static final Set<String> VALID_STATUSES = Set.of("free", "occupied", "billing", "maintenance");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;

    private String name;

    private int seats = 4;

    private String status = "free";

    /** No single customer owns this table (e.g. the branch's fallback for walk-ins) —
     * {@code ordering.OrderService} must never mark it occupied or reuse a prior order for it. */
    @Column(name = "is_virtual", nullable = false)
    private boolean virtual = false;

    public void setFloor(Floor floor) {
        this.floor = floor;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSeats(int seats) {
        this.seats = seats;
    }

    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }

    public void changeStatus(String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw ApiException.badRequest("Statut de table inconnu : " + status);
        }
        this.status = status;
    }
}
