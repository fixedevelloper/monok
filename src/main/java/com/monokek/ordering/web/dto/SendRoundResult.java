package com.monokek.ordering.web.dto;

import java.util.List;

/** Order + which items (if any) couldn't be routed to a kitchen station — nothing is dropped silently. */
public record SendRoundResult(OrderDto order, List<SkippedItem> skippedKitchenItems) {

    public record SkippedItem(Long orderItemId, Long productId, String productName, String reason) {
    }
}
