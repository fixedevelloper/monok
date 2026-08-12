package com.monokek.ordering.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface ReservationRepository extends Repository<Reservation, Long> {

    Reservation save(Reservation reservation);

    Optional<Reservation> findById(Long id);

    void deleteById(Long id);

    List<Reservation> findByCustomerId(Long customerId);

    Page<Reservation> findAllByOrderByPickupDateAsc(Pageable pageable);

    Page<Reservation> findByPickupDateBetweenOrderByPickupDateAsc(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Reservation> findByPickupDateAfterOrderByPickupDateAsc(LocalDateTime now, Pageable pageable);

    Page<Reservation> findByPickupDateBeforeOrderByPickupDateAsc(LocalDateTime now, Pageable pageable);
}
