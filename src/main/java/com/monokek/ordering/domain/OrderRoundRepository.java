package com.monokek.ordering.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface OrderRoundRepository extends Repository<OrderRound, Long> {

    OrderRound save(OrderRound round);

    Optional<OrderRound> findById(Long id);

    List<OrderRound> findByOrderId(Long orderId);
}
