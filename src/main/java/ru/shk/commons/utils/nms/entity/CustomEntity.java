package ru.shk.commons.utils.nms.entity;

import lombok.Getter;
import lombok.SneakyThrows;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.entity.EntityLiving;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.shk.commons.Commons;
import ru.shk.commons.utils.nms.FieldMappings;
import ru.shk.commons.utils.nms.NMSItemSlot;
import ru.shk.commons.utils.nms.PacketUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CustomEntity<T extends CustomEntity> {
    protected List<Player> receivers = new ArrayList<>();
    protected Object entity;
    @Getter protected HashMap<NMSItemSlot, ItemStack> equipment;
    protected boolean isSpawned = false;

    @SneakyThrows
    public CustomEntity(String entityClass, World world, double x, double y, double z){
        entity = ConstructorUtils.invokeConstructor(Class.forName(entityClass), new Object[]{PacketUtil.getNMSWorld(world), x, y, z});
    }

    @SneakyThrows
    public double locX(){
        return (double) entity.getClass().getMethod(FieldMappings.ENTITY_LOCX.getField()).invoke(entity);
    }

    @SneakyThrows
    public double locY(){
        return (double) entity.getClass().getMethod(FieldMappings.ENTITY_LOCY.getField()).invoke(entity);
    }

    @SneakyThrows
    public double locZ(){
        return (double) entity.getClass().getMethod(FieldMappings.ENTITY_LOCZ.getField()).invoke(entity);
    }

    public T equip(NMSItemSlot slot, ItemStack item){
        if(equipment==null) equipment = new HashMap<>();
        equipment.put(slot, item);
        if(isSpawned) equipment();
        return (T) this;
    }

    @SneakyThrows
    public void teleport(double x, double y, double z) {
        entity.getClass().getMethod(FieldMappings.ENTITY_SET_POS.getField(), double.class, double.class, double.class).invoke(entity, x, y, z);
        if(isSpawned) receivers.forEach(this::sendTeleportPacket);
    }
    @SneakyThrows
    public T nameVisible(boolean value) {
        entity.getClass().getMethod(FieldMappings.ENTITY_SETCUSTOMNAMEVISIBLE.getField(), boolean.class).invoke(entity, value);
        return (T) this;
    }
    @SneakyThrows
    public T displayName(String name){
        entity.getClass().getMethod(FieldMappings.ENTITY_SETCUSTOMNAME.getField(), IChatBaseComponent.class).invoke(entity, IChatBaseComponent.a(Commons.getInstance().colorize(name)));
        if(isSpawned) metadata();
        return (T) this;
    }

    public T receivers(List<Player> receivers){
        this.receivers = receivers;
        if(isSpawned) receivers.forEach(this::spawn);
        return (T) this;
    }

    public void removeReceiver(Player p){
        this.receivers.remove(p);
        if(isSpawned) despawn(p);
    }

    public T receiver(Player p){
        this.receivers.add(p);
        if(isSpawned) spawn(p);
        return (T) this;
    }

    // ONLY if it is EntityLiving
    @SneakyThrows
    public T collides(boolean value) {
        Field f = entity.getClass().getDeclaredField("collides");
        f.setAccessible(true);
        f.set(entity, value);
        return (T) this;
    }

    public void spawn(){
        receivers.forEach(player -> {
            sendSpawnPacket(player);
            sendMetadataPacket(player);
            sendEquipmentPacket(player);
        });
        isSpawned = true;
    }

    public void spawn(Player player){
        sendSpawnPacket(player);
        sendMetadataPacket(player);
        sendEquipmentPacket(player);
    }

    public void metadata(){
        receivers.forEach(this::sendMetadataPacket);
    }
    public void equipment(){
        receivers.forEach(this::sendEquipmentPacket);
    }

    public void metadata(Player player){
        sendMetadataPacket(player);
    }

    public void despawn(){
        receivers.forEach(this::sendDespawnPacket);
        isSpawned = false;
    }

    public void despawn(Player player){
        sendDespawnPacket(player);
    }

    protected void sendSpawnPacket(Player p){
        PacketUtil.spawnLivingEntity(p, entity);
    }
    protected void sendMetadataPacket(Player p){
        PacketUtil.entityMetadata(p, entity);
    }
    protected void sendEquipmentPacket(Player p){
        PacketUtil.equipEntity(p, entity, equipment);
    }
    protected void sendTeleportPacket(Player p){
        PacketUtil.spawnEntity(p, entity);
    }
    protected void sendDespawnPacket(Player p){
        PacketUtil.destroyEntity(p, entity);
    }

}
