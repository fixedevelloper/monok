package com.monokek.ordering.infrastructure;

import com.monokek.ordering.domain.Reservation;
import com.monokek.ordering.domain.ReservationRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaReservationRepository extends ReservationRepository, JpaRepository<Reservation, Long> {
}
