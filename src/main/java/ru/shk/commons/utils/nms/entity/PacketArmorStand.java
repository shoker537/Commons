package ru.shk.commons.utils.nms.entity;

import lombok.SneakyThrows;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.bukkit.World;
import ru.shk.commons.utils.nms.FieldMappings;
import ru.shk.commons.utils.nms.ReflectionUtil;

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
        ReflectionUtil.runMethod(entity, FieldMappings.ARMORSTAND_SETHEADPOSE.getField(), vector);
        return this;
    }
    @SneakyThrows
    public PacketArmorStand bodyPose(float x, float y, float z){
        Class<?> vectorClass = Class.forName("net.minecraft.core.Vector3f");
        Object vector = ConstructorUtils.invokeConstructor(vectorClass, new Object[]{x, y, z});
        ReflectionUtil.runMethod(entity, FieldMappings.ARMORSTAND_SETBODYPOSE.getField(), vector);
        return this;
    }
    @SneakyThrows
    public PacketArmorStand leftArmPose(float x, float y, float z){
        Class<?> vectorClass = Class.forName("net.minecraft.core.Vector3f");
        Object vector = ConstructorUtils.invokeConstructor(vectorClass, new Object[]{x, y, z});
        ReflectionUtil.runMethod(entity, FieldMappings.ARMORSTAND_SETLEFTARMPOSE.getField(), vector);
        return this;
    }
    @SneakyThrows
    public PacketArmorStand rightArmPose(float x, float y, float z){
        Class<?> vectorClass = Class.forName("net.minecraft.core.Vector3f");
        Object vector = ConstructorUtils.invokeConstructor(vectorClass, new Object[]{x, y, z});
        ReflectionUtil.runMethod(entity, FieldMappings.ARMORSTAND_SETRIGHTARMPOSE.getField(), vector);
        return this;
    }
    @SneakyThrows
    public PacketArmorStand leftLegPose(float x, float y, float z){
        Class<?> vectorClass = Class.forName("net.minecraft.core.Vector3f");
        Object vector = ConstructorUtils.invokeConstructor(vectorClass, new Object[]{x, y, z});
        ReflectionUtil.runMethod(entity, FieldMappings.ARMORSTAND_SETLEFTLEGPOSE.getField(), vector);
        return this;
    }
    @SneakyThrows
    public PacketArmorStand rightLegPose(float x, float y, float z){
        Class<?> vectorClass = Class.forName("net.minecraft.core.Vector3f");
        Object vector = ConstructorUtils.invokeConstructor(vectorClass, new Object[]{x, y, z});
        ReflectionUtil.runMethod(entity, FieldMappings.ARMORSTAND_SETRIGHTLEGPOSE.getField(), vector);
        return this;
    }
}
