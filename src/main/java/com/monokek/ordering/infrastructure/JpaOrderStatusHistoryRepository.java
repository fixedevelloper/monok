package com.monokek.ordering.infrastructure;

import com.monokek.ordering.domain.OrderStatusHistory;
import com.monokek.ordering.domain.OrderStatusHistoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaOrderStatusHistoryRepository extends OrderStatusHistoryRepository, JpaRepository<OrderStatusHistory, Long> {
}
