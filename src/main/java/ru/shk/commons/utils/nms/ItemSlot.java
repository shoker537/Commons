package ru.shk.commons.utils.nms;

import org.bukkit.inventory.EquipmentSlot;

public enum ItemSlot {
    HEAD(net.minecraft.world.entity.EquipmentSlot.HEAD, EquipmentSlot.HEAD),
    CHEST(net.minecraft.world.entity.EquipmentSlot.CHEST, EquipmentSlot.CHEST),
    LEGS(net.minecraft.world.entity.EquipmentSlot.LEGS, EquipmentSlot.LEGS),
    FEET(net.minecraft.world.entity.EquipmentSlot.FEET, EquipmentSlot.FEET),
    MAIN_HAND(net.minecraft.world.entity.EquipmentSlot.MAINHAND, EquipmentSlot.HAND),
    OFF_HAND(net.minecraft.world.entity.EquipmentSlot.OFFHAND, EquipmentSlot.OFF_HAND);

    private final net.minecraft.world.entity.EquipmentSlot nmsSlot;

    private final EquipmentSlot bukkitSlot;

    public net.minecraft.world.entity.EquipmentSlot getNmsSlot() {
        return this.nmsSlot;
    }

    public EquipmentSlot getBukkitSlot() {
        return this.bukkitSlot;
    }

    ItemSlot(net.minecraft.world.entity.EquipmentSlot nmsSlot, EquipmentSlot bukkitSlot) {
        this.nmsSlot = nmsSlot;
        this.bukkitSlot = bukkitSlot;
    }
}
