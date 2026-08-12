package com.monokek.kitchen.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kitchen_tickets")
@Getter
@Setter
@NoArgsConstructor
public class KitchenTicket extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** References ordering.OrderRound by id only — see module package-info. */
    @Column(name = "order_round_id", nullable = false)
    private Long orderRoundId;

    /** References ordering.Order by id only. */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private KitchenStation station;

    /** e.g. "Supplément : +2 Frites" — see V3__add_kitchen_ticket_note.sql for why this exists. */
    @Column(columnDefinition = "TEXT")
    private String note;

    /** pending, preparing, ready, served */
    private String status = "pending";

    private int priority = 1;
}
