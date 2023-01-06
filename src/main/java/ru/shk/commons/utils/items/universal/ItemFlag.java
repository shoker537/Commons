package ru.shk.commons.utils.items.universal;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public enum ItemFlag {
    HIDE_ENCHANTMENTS(1, "HIDE_ENCHANTS"),
    HIDE_MODIFIERS(2, "HIDE_ATTRIBUTES"),
    HIDE_UNBREAKABLE(4, "HIDE_UNBREAKABLE"),
    HIDE_CAN_DESTROY(8, "HIDE_DESTROYS"),
    HIDE_CAN_PLACE(16, "HIDE_PLACED_ON"),
    HIDE_ADDITIONAL(32, "HIDE_POTION_EFFECTS"),
    HIDE_DYE(64, "HIDE_DYE");

    private final int value;
    @Getter@Accessors(fluent = true)
    private final String bukkitName;

    ItemFlag(int value, @NonNull String bukkitName) {
        this.value = value;
        this.bukkitName = bukkitName;
    }

    public static int asInt(@NonNull List<ItemFlag> flags){
        return flags.stream().mapToInt(value1 -> value1.value).sum();
    }

    @NonNull
    public static List<ItemFlag> fromInt(int flagsSumm){
        List<ItemFlag> flags = new ArrayList<>();
        for (ItemFlag flag : values()) {
            if(hasItemFlag(flag, flagsSumm)) flags.add(flag);
        }
        return flags;
    }

    public static boolean hasItemFlag(@NonNull ItemFlag flag, int flags) {
        int bitModifier = flag.value;
        return (flags & bitModifier) == bitModifier;
    }

    @Nullable
    public static ItemFlag fromBukkit(@NonNull String bukkitName){
        for (ItemFlag flag : values()) {
            if(flag.bukkitName.equalsIgnoreCase(bukkitName)) return flag;
        }
        return null;
    }
}
