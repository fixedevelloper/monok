package com.monokek.ordering.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Mirrors {@code App\Http\Resources\OrderResource} (+ nested Round/Item/Modifier resources). */
public record OrderDto(
        Long id,
        UUID uuid,
        String reference,
        String type,
        String status,
        Amounts amounts,
        TableRef table,
        PersonRef waiter,
        PersonRef cashier,
        List<RoundDto> rounds,
        String note,
        String time,
        String date
) {
    public record Amounts(BigDecimal subtotal, BigDecimal tax, BigDecimal discount, BigDecimal total, String formattedTotal) {
    }

    public record TableRef(Long id, String name, String status) {
    }

    public record PersonRef(Long id, String name) {
    }

    public record RoundDto(
            Long id, int roundNumber, String status, String note, String sentAt, List<ItemDto> items, BigDecimal totalRound,
            String voidReason) {
    }

    public record ItemDto(
            Long id, int qty, BigDecimal price, BigDecimal total, String status,
            ProductRef product, VariantRef variant, List<ModifierDto> modifiers, String formattedTotal) {
    }

    public record ProductRef(Long id, String name) {
    }

    public record VariantRef(Long id, String name) {
    }

    public record ModifierDto(Long id, Long modifierItemId, int quantity, String name, BigDecimal price) {
    }
}
