package ru.shk.commons.utils.nms.entity.mob;

import net.minecraft.world.entity.animal.chicken.Chicken;
import org.bukkit.Location;
import org.bukkit.World;
import ru.shk.commons.utils.nms.entity.PacketEntity;

public class PacketChicken extends PacketEntity<PacketChicken> {
    public PacketChicken(World world, double x, double y, double z) {
        super(Chicken.class.getName(), "chicken", world, x, y, z);
    }

    public PacketChicken(Location l) {
        super(Chicken.class.getName(), "chicken", l);
    }

    public PacketChicken(World world) {
        super(Chicken.class.getName(), "chicken", world);
    }
}
