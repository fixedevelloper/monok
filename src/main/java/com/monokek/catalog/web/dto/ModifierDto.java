package com.monokek.catalog.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** Mirrors {@code App\Http\Resources\ModifierResource}/{@code ModifierItemResource}. */
public record ModifierDto(Long id, String name, List<Item> items) {

    public record Item(Long id, String name, BigDecimal price) {
    }
}
