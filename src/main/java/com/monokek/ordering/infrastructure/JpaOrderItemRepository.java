package com.monokek.ordering.infrastructure;

import com.monokek.ordering.domain.OrderItem;
import com.monokek.ordering.domain.OrderItemRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaOrderItemRepository extends OrderItemRepository, JpaRepository<OrderItem, Long> {
}
