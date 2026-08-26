package com.monokek.catalog.domain;

import com.monokek.common.Timestamps;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** An option group, e.g. "extras". {@code type} ("accompaniment"/"supplement") plus {@code
 * required}/{@code minSelect}/{@code maxSelect} replace what the POS used to infer implicitly —
 * "the first group attached to a product is the included accompaniment, any item priced above
 * zero anywhere is a paid supplement" (see the old {@code ModifierModal.tsx}) — with an explicit,
 * admin-configured rule {@code ordering.OrderService} can actually enforce server-side. */
@Entity
@Table(name = "modifiers")
@Getter
@NoArgsConstructor
public class Modifier extends Timestamps {

    /** "accompaniment" or "supplement" — see class doc. */
    public static final String TYPE_ACCOMPANIMENT = "accompaniment";
    public static final String TYPE_SUPPLEMENT = "supplement";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String type = TYPE_SUPPLEMENT;

    private boolean required = false;

    /** Minimum total quantity that must be picked across this group's items — 0 means "no minimum
     * beyond {@code required}" (which itself just means "at least 1" when {@code minSelect} is 0). */
    @Column(name = "min_select")
    private int minSelect = 0;

    /** Maximum total quantity that can be picked across this group's items. Null means unlimited. */
    @Column(name = "max_select")
    private Integer maxSelect;

    @OneToMany(mappedBy = "modifier", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ModifierItem> items = new ArrayList<>();

    public static Modifier create(String name, String type, boolean required, int minSelect, Integer maxSelect) {
        Modifier modifier = new Modifier();
        modifier.name = name;
        modifier.configure(type, required, minSelect, maxSelect);
        return modifier;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void configure(String type, boolean required, int minSelect, Integer maxSelect) {
        if (!TYPE_ACCOMPANIMENT.equals(type) && !TYPE_SUPPLEMENT.equals(type)) {
            throw new IllegalArgumentException("Type de groupe inconnu : " + type);
        }
        if (maxSelect != null && maxSelect < minSelect) {
            throw new IllegalArgumentException("Le maximum sélectionnable ne peut pas être inférieur au minimum.");
        }
        this.type = type;
        this.required = required;
        this.minSelect = minSelect;
        this.maxSelect = maxSelect;
    }

    /** The effective minimum a selection must satisfy — {@code required} alone (with no explicit
     * {@code minSelect}) means "at least one". */
    public int effectiveMinSelect() {
        return required ? Math.max(minSelect, 1) : minSelect;
    }

    public ModifierItem addItem(String name, BigDecimal price) {
        ModifierItem item = ModifierItem.of(this, name, price);
        items.add(item);
        return item;
    }
}
