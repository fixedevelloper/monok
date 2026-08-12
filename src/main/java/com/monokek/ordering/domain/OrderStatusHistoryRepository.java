package com.monokek.ordering.domain;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface OrderStatusHistoryRepository extends Repository<OrderStatusHistory, Long> {

    OrderStatusHistory save(OrderStatusHistory history);

    Optional<OrderStatusHistory> findById(Long id);

    List<OrderStatusHistory> findByOrderId(Long orderId);
}
