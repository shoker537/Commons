package ru.shk.commons.utils.items.universal;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public enum EnchantmentType {

    PROTECTION_ENVIRONMENTAL("protection"),
    PROTECTION_FIRE("fire_protection"),
    PROTECTION_FALL("feather_falling"),
    PROTECTION_EXPLOSIONS("blast_protection"),
    PROTECTION_PROJECTILE("projectile_protection"),
    RESPIRATION,
    AQUA_AFFINITY,
    THORNS,
    DEPTH_STRIDER,
    FROST_WALKER,
    BINDING_CURSE,
    SHARPNESS,
    DAMAGE_UNDEAD("smite"),
    DAMAGE_ARTHROPODS("bane_of_arthropods"),
    KNOCKBACK,
    FIRE_ASPECT,
    LOOTING("looting"),
    SWEEPING_EDGE("sweeping"),
    EFFICIENCY,
    SILK_TOUCH,
    UNBREAKING,
    FORTUNE,
    ARROW_DAMAGE("power"),
    ARROW_KNOCKBACK("punch"),
    ARROW_FIRE("flame"),
    ARROW_INFINITY("infinity"),
    LUCK_OF_THE_SEA,
    LURE,
    LOYALTY,
    IMPALING,
    RIPTIDE,
    CHANNELING,
    MULTISHOT,
    QUICK_CHARGE,
    PIERCING,
    MENDING,
    VANISHING_CURSE,
    SOUL_SPEED,
    SWIFT_SNEAK
    ;

    private final String namespacedKey;

    EnchantmentType(){
        namespacedKey = null;
    }

    EnchantmentType(@Nullable String namespacedKey) {
        this.namespacedKey = namespacedKey;
    }

    public String namespacedKey(){
        return namespacedKey==null?name().toLowerCase():namespacedKey;
    }

    @Nullable
    public static EnchantmentType fromString(@NonNull String s){
        for (EnchantmentType value : values()) {
            if(value.name().equalsIgnoreCase(s) || value.namespacedKey().equalsIgnoreCase(s)) return value;
        }

        return null;
    }
}
