package com.monokek.ordering.domain;

import com.monokek.ordering.domain.event.OrderStatusChangedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    void reopensAndRecompletesWhenARoundIsSentAfterTheOrderAlreadyCompleted() {
        Order order = Order.openForTable(1L, 10L, 2L, 2L);
        order.clearDomainEvents();

        OrderRound round1 = order.openRound(null);
        ReflectionTestUtils.setField(round1, "id", 100L);
        order.clearDomainEvents();

        order.applyKitchenRoundStatus(100L, "served", null);
        assertThat(order.getStatus()).isEqualTo("completed");
        assertThat(statusChanges(order)).containsExactly("pending->completed");
        order.clearDomainEvents();

        // A guest orders another round before the bill: opening it on an already-"completed"
        // order must reopen the order — see Order#openRound's javadoc for why this used to stay
        // stuck on "completed" (billable in the POS) for the whole time this new round cooked.
        OrderRound round2 = order.openRound(null);
        ReflectionTestUtils.setField(round2, "id", 101L);

        assertThat(order.getStatus()).isEqualTo("pending");
        assertThat(statusChanges(order)).containsExactly("completed->pending");
        order.clearDomainEvents();

        order.applyKitchenRoundStatus(101L, "served", null);

        assertThat(order.getStatus()).isEqualTo("completed");
        assertThat(statusChanges(order)).containsExactly("pending->completed");
    }

    private static List<String> statusChanges(Order order) {
        return order.domainEvents().stream()
                .filter(OrderStatusChangedEvent.class::isInstance)
                .map(OrderStatusChangedEvent.class::cast)
                .map(e -> e.previousStatus() + "->" + e.newStatus())
                .toList();
    }
}
