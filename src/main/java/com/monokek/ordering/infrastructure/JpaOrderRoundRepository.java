package com.monokek.ordering.infrastructure;

import com.monokek.ordering.domain.OrderRound;
import com.monokek.ordering.domain.OrderRoundRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaOrderRoundRepository extends OrderRoundRepository, JpaRepository<OrderRound, Long> {
}
