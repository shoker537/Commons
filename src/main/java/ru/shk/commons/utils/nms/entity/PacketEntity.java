package ru.shk.commons.utils.nms.entity;

import lombok.Getter;
import lombok.SneakyThrows;
import net.minecraft.network.chat.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.shk.commons.Commons;
import ru.shk.commons.utils.nms.FieldMappings;
import ru.shk.commons.utils.nms.ItemSlot;
import ru.shk.commons.utils.nms.PacketUtil;
import ru.shk.commons.utils.nms.ReflectionUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PacketEntity<T extends PacketEntity> {
    protected List<Player> receivers = new ArrayList<>();
    protected Object entity;
    @Getter protected HashMap<ItemSlot, ItemStack> equipment;
    protected boolean isSpawned = false;
    @Getter private boolean isTicking = false;
    @Getter private boolean isValid = true;

    @SneakyThrows
    public PacketEntity(String entityClass, World world, double x, double y, double z){
        entity = ReflectionUtil.constructObject(Class.forName(entityClass), PacketUtil.getNMSWorld(world), x, y, z);
    }

    @SneakyThrows
    public double locX(){
        return (double) ReflectionUtil.runMethod(entity, FieldMappings.ENTITY_LOCX.getField());
    }

    @SneakyThrows
    public double locY(){
        return (double) ReflectionUtil.runMethod(entity, FieldMappings.ENTITY_LOCY.getField());
    }

    @SneakyThrows
    public double locZ(){
        return (double) ReflectionUtil.runMethod(entity, FieldMappings.ENTITY_LOCZ.getField());
    }

    public T equip(ItemSlot slot, ItemStack item){
        if(equipment==null) equipment = new HashMap<>();
        equipment.put(slot, item);
        if(isSpawned) equipment();
        return (T) this;
    }

    @SneakyThrows
    public void teleport(double x, double y, double z) {
        ReflectionUtil.runMethod(entity, FieldMappings.ENTITY_SET_POS.getField(),x, y, z);
        if(isSpawned) receivers.forEach(this::sendTeleportPacket);
    }
    @SneakyThrows
    public T nameVisible(boolean value) {
        ReflectionUtil.runMethod(entity, FieldMappings.ENTITY_SETCUSTOMNAMEVISIBLE.getField(),value);
        return (T) this;
    }
    @SneakyThrows
    public T displayName(String name){
        ReflectionUtil.runMethod(entity, FieldMappings.ENTITY_SETCUSTOMNAME.getField(), Component.literal(Commons.colorizeWithHex(name)));
        if(isSpawned) metadata();
        return (T) this;
    }
    public T tick(boolean value){
        this.isTicking = value;
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
        ReflectionUtil.setField(entity, "collides", value);
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

    public void tick(){

    }

}
