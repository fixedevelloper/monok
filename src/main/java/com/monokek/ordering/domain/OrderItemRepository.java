package com.monokek.ordering.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface OrderItemRepository extends Repository<OrderItem, Long> {

    OrderItem save(OrderItem item);

    Optional<OrderItem> findById(Long id);

    List<OrderItem> findByOrderRoundId(Long orderRoundId);
}
