package ru.shk.commons.utils.nms;

import org.bukkit.entity.Entity;

public class EntityUtils {
    public static void move(Entity e, double x, double y, double z){
        net.minecraft.world.entity.Entity entity = (net.minecraft.world.entity.Entity) PacketUtil.getNMSEntity(e);
        entity.snapTo(x,y,z);
    }

    public static void move(Entity e, double x, double y, double z, float yaw, float pitch){
        net.minecraft.world.entity.Entity entity = (net.minecraft.world.entity.Entity) PacketUtil.getNMSEntity(e);
        entity.snapTo(x,y,z,yaw,pitch);
    }

    public static void rotate(Entity e, float yaw, float pitch){
        net.minecraft.world.entity.Entity entity = (net.minecraft.world.entity.Entity) PacketUtil.getNMSEntity(e);
        entity.forceSetRotation(yaw, pitch);
    }
}
