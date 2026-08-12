package com.monokek.kitchen.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface KitchenTicketRepository extends Repository<KitchenTicket, Long> {

    KitchenTicket save(KitchenTicket ticket);

    Optional<KitchenTicket> findById(Long id);

    List<KitchenTicket> findByStationId(Long stationId);

    List<KitchenTicket> findByOrderId(Long orderId);

    List<KitchenTicket> findByOrderRoundId(Long orderRoundId);

    List<KitchenTicket> findByStationIdAndStatusInOrderByCreatedAtAsc(Long stationId, List<String> statuses);

    long countByStationIdAndStatusIn(Long stationId, List<String> statuses);
}
