package com.monokek.ordering.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface OrderItemModifierRepository extends Repository<OrderItemModifier, Long> {

    OrderItemModifier save(OrderItemModifier modifier);

    Optional<OrderItemModifier> findById(Long id);

    List<OrderItemModifier> findByOrderItemId(Long orderItemId);
}
