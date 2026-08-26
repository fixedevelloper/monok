package com.monokek.catalog.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** Mirrors {@code App\Http\Resources\ModifierResource}/{@code ModifierItemResource}, extended with
 * the group-selection rules {@code Modifier} now carries (type/required/minSelect/maxSelect). */
public record ModifierDto(Long id, String name, String type, boolean required, int minSelect, Integer maxSelect, List<Item> items) {

    public record Item(Long id, String name, BigDecimal price) {
    }
}
