package ru.shk.commons.utils.nms.entity;

import lombok.SneakyThrows;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.bukkit.World;
import ru.shk.commons.utils.nms.FieldMappings;

public class PacketArmorStand extends PacketEntity<PacketArmorStand> {
    public PacketArmorStand(World world, double x, double y, double z){
        super("net.minecraft.world.entity.decoration.EntityArmorStand", "armor_stand", world, x, y, z);
    }
    @SneakyThrows
    public PacketArmorStand small(boolean value) {
        entity.getClass().getMethod(FieldMappings.ARMORSTAND_SETSMALL.getField(), boolean.class).invoke(entity, value);
        return this;
    }
    @SneakyThrows
    public PacketArmorStand basePlate(boolean value) {
        entity.getClass().getMethod(FieldMappings.ARMORSTAND_SETBASEPLATE.getField(), boolean.class).invoke(entity, value);
        return this;
    }
    @SneakyThrows
    public PacketArmorStand invisible(boolean value) {
        entity.getClass().getMethod(FieldMappings.ARMORSTAND_SETINVISIBLE.getField(), boolean.class).invoke(entity, value);
        return this;
    }
    @SneakyThrows
    public PacketArmorStand gravity(boolean value) {
        entity.getClass().getMethod(FieldMappings.ENTITY_SETNOGRAVITY.getField(), boolean.class).invoke(entity, !value);
        return this;
    }
    @SneakyThrows
    public PacketArmorStand arms(boolean value) {
        entity.getClass().getMethod(FieldMappings.ARMORSTAND_SETARMS.getField(), boolean.class).invoke(entity, value);
        return this;
    }
    @SneakyThrows
    public PacketArmorStand marker(boolean value) {
        entity.getClass().getDeclaredMethod(FieldMappings.ARMORSTAND_SETMARKER.getField(), boolean.class).invoke(entity, value);
        return this;
    }
    @SneakyThrows
    public PacketArmorStand headPose(float x, float y, float z){
        Class<?> vectorClass = Class.forName("net.minecraft.core.Vector3f");
        Object vector = ConstructorUtils.invokeConstructor(vectorClass, new Object[]{x, y, z});
        entity.getClass().getMethod(FieldMappings.ARMORSTAND_SETHEADPOSE.getField(), vectorClass).invoke(entity, vector);
        return this;
    }
}
