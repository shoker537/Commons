package ru.shk.commons.utils.items.universal;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter@Accessors(fluent = true, chain = true)
public class Enchantment {
    private final EnchantmentType type;
    @Setter private int level = 1;

    public Enchantment(EnchantmentType type) {
        this.type = type;
    }

    public Enchantment(EnchantmentType type, int level){
        this(type);
        this.level = level;
    }
}
