package com.monokek.ordering.application;

import com.monokek.ordering.OrderRoundStatusUpdater;
import com.monokek.ordering.domain.Order;
import com.monokek.ordering.domain.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OrderRoundStatusUpdaterService implements OrderRoundStatusUpdater {

    private final OrderRepository orderRepository;

    OrderRoundStatusUpdaterService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional
    public void applyKitchenRoundStatus(Long orderId, Long orderRoundId, String resolvedStatus) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.applyKitchenRoundStatus(orderRoundId, resolvedStatus, null);
            orderRepository.save(order);
        });
    }
}
