package ru.shk.commons.utils.nms.entity.mob;

import org.bukkit.Location;
import org.bukkit.World;
import ru.shk.commons.utils.nms.entity.PacketEntity;

public class PacketChicken extends PacketEntity<PacketChicken> {
    public PacketChicken(World world, double x, double y, double z) {
        super("net.minecraft.world.entity.animal.EntityChicken", "chicken", world, x, y, z);
    }

    public PacketChicken(Location l) {
        super("net.minecraft.world.entity.animal.EntityChicken", "chicken", l);
    }

    public PacketChicken(World world) {
        super("net.minecraft.world.entity.animal.EntityChicken", "chicken", world);
    }
}
