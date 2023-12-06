package ru.shk.commons.utils.nms.entity;

import lombok.SneakyThrows;
import net.minecraft.core.Rotations;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import org.bukkit.World;
import ru.shk.commons.utils.nms.PacketUtil;
@SuppressWarnings("unused")
public class PacketArmorStand extends PacketEntity<PacketArmorStand> {
    public PacketArmorStand(World world, double x, double y, double z){
        super("net.minecraft.world.entity.decoration.EntityArmorStand", "armor_stand", world, x, y, z);
    }

    @Override
    public void createEntity(World world) {
        entity = new ArmorStand(EntityType.ARMOR_STAND, (Level) PacketUtil.getNMSWorld(world));
    }

    @SneakyThrows
    public PacketArmorStand small(boolean value) {
        ((ArmorStand)entity).setSmall(value);
        return this;
    }
    @SneakyThrows
    public PacketArmorStand basePlate(boolean value) {
        ((ArmorStand)entity).setNoBasePlate(value);
        return this;
    }
    @SneakyThrows
    public PacketArmorStand invisible(boolean value) {
        entity.setInvisible(value);
        return this;
    }
    @SneakyThrows
    public PacketArmorStand gravity(boolean value) {
        entity.setNoGravity(!value);
        return this;
    }
    @SneakyThrows
    public PacketArmorStand arms(boolean value) {
        ((ArmorStand)entity).setShowArms(value);
        return this;
    }
    @SneakyThrows
    public PacketArmorStand marker(boolean value) {
        ((ArmorStand)entity).setMarker(value);
        return this;
    }
    @SneakyThrows
    public PacketArmorStand headPose(float x, float y, float z){
        ((ArmorStand)entity).setHeadPose(new Rotations(x,y,z));
        return this;
    }
    @SneakyThrows
    public PacketArmorStand bodyPose(float x, float y, float z){
        ((ArmorStand)entity).setBodyPose(new Rotations(x,y,z));
        return this;
    }
    @SneakyThrows
    public PacketArmorStand leftArmPose(float x, float y, float z){
        ((ArmorStand)entity).setLeftArmPose(new Rotations(x,y,z));
        return this;
    }
    @SneakyThrows
    public PacketArmorStand rightArmPose(float x, float y, float z){
        ((ArmorStand)entity).setRightArmPose(new Rotations(x,y,z));
        return this;
    }
    @SneakyThrows
    public PacketArmorStand leftLegPose(float x, float y, float z){
        ((ArmorStand)entity).setLeftLegPose(new Rotations(x,y,z));
        return this;
    }
    @SneakyThrows
    public PacketArmorStand rightLegPose(float x, float y, float z){
        ((ArmorStand)entity).setRightLegPose(new Rotations(x,y,z));
        return this;
    }
}
