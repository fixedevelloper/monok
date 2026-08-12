package com.monokek.ordering.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
public class Reservation extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** References crm.Customer by id only. */
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "pickup_date", nullable = false)
    private LocalDateTime pickupDate;

    @Column(name = "guests_count")
    private int guestsCount = 1;

    @Column(name = "manager_notes", columnDefinition = "TEXT")
    private String managerNotes;

    /** confirmed, arrived, no_show */
    @Column(name = "reservation_status")
    private String reservationStatus = "confirmed";
}
