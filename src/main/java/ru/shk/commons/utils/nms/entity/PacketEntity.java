package ru.shk.commons.utils.nms.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.shk.commons.Commons;
import ru.shk.commons.utils.nms.FieldMappings;
import ru.shk.commons.utils.nms.ItemSlot;
import ru.shk.commons.utils.nms.PacketUtil;
import ru.shk.commons.utils.nms.ReflectionUtil;

import java.util.*;

public class PacketEntity<T extends PacketEntity> {
    protected final List<Player> receivers = new ArrayList<>();
    protected Object entity;
    @Getter protected HashMap<ItemSlot, ItemStack> equipment;
    protected boolean isSpawned = false;
    @Getter private boolean isTicking = false;
    @Getter@Setter private int showInRadius = -1;
    @Getter@Setter private boolean isValid = true;

    @SneakyThrows
    public PacketEntity(String entityClass, String entityTypeId, World world, double x, double y, double z){
        this(entityClass, entityTypeId, world);
        teleport(x,y,z);
    }
//    private static String entityTypeName(String entityClass){
//        String[] a = entityClass.split("\\.");
//        String r = a[a.length-1];
//        if(!r.contains("$")) return r.toUpperCase();
//        a = r.split("\\$");
//        return a[a.length-1].toUpperCase();
//    }
    @SneakyThrows
    public PacketEntity(String entityClass, String entityTypeEnum, World world){
        entity = ReflectionUtil.constructObject(Class.forName(entityClass), ((Optional<?>)Class.forName("net.minecraft.world.entity.EntityTypes").getMethod(FieldMappings.ENTITYTYPE_BYSTRING.getField(), String.class).invoke(null, entityTypeEnum)).get(), PacketUtil.getNMSWorld(world));
        spawn();
    }

    public World getWorld(){
        return Bukkit.getWorld(level().getWorld().getUID());
    }
    public Location getLocation(){
        return new Location(getWorld(), locX(), locY(), locZ());
    }

    @SneakyThrows
    private Level level(){
        return (Level) entity.getClass().getMethod(FieldMappings.ENTITY_GETLEVEL.getField()).invoke(entity);
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

    public List<Entity> getNearbyEntities(int radiusInChunks){
        Location l = getLocation();
        int chunkX = l.getBlockX()/16;
        int chunkZ = l.getBlockZ()/16;
        if(!l.getWorld().isChunkLoaded(chunkX, chunkZ)) return new ArrayList<>();
        List<Entity> list = new ArrayList<>();
        for (int x = -radiusInChunks; x <= radiusInChunks; x++) {
            for (int z = -radiusInChunks; z <= radiusInChunks; z++) {
                int finalX = x+chunkX;
                int finalZ = z+chunkZ;
                if(!l.getWorld().isChunkLoaded(finalX, finalZ)) continue;
                try {
                    Chunk c = l.getWorld().getChunkAt(finalX,finalZ, false);
                    list.addAll(Arrays.asList(c.getEntities()));
                } catch (Throwable t){}
            }
        }
        return list;
    }
    public List<Entity> getNearbyEntities(int radiusInChunks, EntityType ofType){
        return getNearbyEntities(radiusInChunks).stream().filter(entity1 -> entity1.getType()==ofType).toList();
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
        this.receivers.clear();
        this.receivers.addAll(receivers);
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
        if(equipment!=null && equipment.size()!=0) sendEquipmentPacket(player);
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

    protected int visibilityTickCounter = 0;
    public void tick(){


        if(showInRadius==-1) return;
        visibilityTickCounter++;
//        Bukkit.broadcastMessage("VisibilityCounter: "+visibilityTickCounter);
        if(visibilityTickCounter==20) {
            visibilityTickCounter = 0;
            visibilityTick();
        }
    }

    public void visibilityTick(){
        synchronized (receivers){
            List<Player> list = getNearbyEntities(showInRadius, EntityType.PLAYER).stream().map(entity1 -> (Player) entity1).toList();
            List<Player> toRemove = new ArrayList<>();
            List<Player> toAdd = new ArrayList<>();
            receivers.forEach(player -> {
                if(list.stream().noneMatch(p -> p.getUniqueId().equals(player.getUniqueId()))) {
                    toRemove.add(player);
                }
            });
            list.forEach(player -> {
                if(receivers.stream().noneMatch(p -> p.getUniqueId().equals(player.getUniqueId()))) {
                    toAdd.add(player);
                }
            });
            toRemove.forEach(this::removeReceiver);
            toAdd.forEach(this::receiver);
        }
    }

    @SneakyThrows
    public Object getEntityData(){
        return entity.getClass().getMethod(FieldMappings.ENTITY_GETDATAWATCHER.getField()).invoke(entity);
    }

}
