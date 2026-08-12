package com.monokek.printing.application.dto;

import java.util.List;

/** {@code PrintQueue.content}, deserialized — everything {@link com.monokek.printing.application.EscPosTicketRenderer} needs for a kitchen/bar ticket. */
public record KitchenTicketContent(Long orderId, Long roundId, String tableName, String serverName, String note, List<TicketItem> items) {

    public record TicketItem(String productName, int qty, List<String> modifierNames) {
    }
}
