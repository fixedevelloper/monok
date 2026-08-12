package com.monokek.catalog.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** An option group, e.g. "extras". */
@Entity
@Table(name = "modifiers")
@Getter
@NoArgsConstructor
public class Modifier extends Timestamps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "modifier", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ModifierItem> items = new ArrayList<>();

    public static Modifier create(String name) {
        Modifier modifier = new Modifier();
        modifier.name = name;
        return modifier;
    }

    public void rename(String name) {
        this.name = name;
    }

    public ModifierItem addItem(String name, BigDecimal price) {
        ModifierItem item = ModifierItem.of(this, name, price);
        items.add(item);
        return item;
    }
}
