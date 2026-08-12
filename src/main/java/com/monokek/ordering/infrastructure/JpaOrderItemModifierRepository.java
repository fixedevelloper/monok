package com.monokek.ordering.infrastructure;

import com.monokek.ordering.domain.OrderItemModifier;
import com.monokek.ordering.domain.OrderItemModifierRepository;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaOrderItemModifierRepository extends OrderItemModifierRepository, JpaRepository<OrderItemModifier, Long> {
}
