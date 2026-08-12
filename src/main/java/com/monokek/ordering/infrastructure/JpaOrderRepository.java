package com.monokek.ordering.infrastructure;

import com.monokek.ordering.domain.Order;
import com.monokek.ordering.domain.OrderRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaOrderRepository extends OrderRepository, JpaRepository<Order, Long> {
}
