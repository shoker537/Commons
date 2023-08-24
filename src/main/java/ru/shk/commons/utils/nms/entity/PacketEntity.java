package ru.shk.commons.utils.nms.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.val;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
import ru.shk.commons.utils.Logger;
import ru.shk.commons.utils.nms.FieldMappings;
import ru.shk.commons.utils.nms.ItemSlot;
import ru.shk.commons.utils.nms.PacketUtil;
import ru.shk.commons.utils.nms.ReflectionUtil;

import java.util.*;

@SuppressWarnings({"unused", "unchecked"})
public class PacketEntity<T extends PacketEntity> {
    protected final List<Player> receivers = new ArrayList<>();
    protected final List<Player> excludedReceivers = new ArrayList<>();
    private final String entityClass;
    private final String entityTypeEnum;
    @Getter protected net.minecraft.world.entity.Entity entity;
    @Getter protected HashMap<ItemSlot, ItemStack> equipment;
    protected boolean isSpawned = false;
    @Getter private boolean isTicking = false;
    @Getter@Setter private int showInRadius = -1;
    @Getter@Setter private boolean isValid = true;
    private boolean leashHolderSet = false;
    protected static boolean compatibility;

    static {
        compatibility = !Commons.isVersionLatestCompatible();
        Logger.warning("Compatibility mode: "+compatibility);
    }

    @SneakyThrows
    public PacketEntity(String entityClass, String entityTypeId, World world, double x, double y, double z){
        this(entityClass, entityTypeId, world);
        teleport(x,y,z);
    }

    public PacketEntity(String entityClass, String entityTypeId, Location l){
        this(entityClass, entityTypeId, l.getWorld(), l.getX(), l.getY(), l.getZ());
    }

    @SneakyThrows
    public void leashHolder(Entity e){
        leashHolderSet = e!=null;
        if(compatibility) ReflectionUtil.runMethod(Mob.class, entity, FieldMappings.MOB_SETLEASHEDTO.getField(), PacketUtil.getNMSEntity(e), true);
        ((Mob)entity).setLeashedTo((net.minecraft.world.entity.Entity) PacketUtil.getNMSEntity(e), true);
    }

    @SneakyThrows
    public Entity leashHolder(){
        try {
            if(compatibility){
                if(!(entity instanceof Mob)) return null;
                Object o = ReflectionUtil.runMethod(Mob.class, entity, FieldMappings.MOB_GETLEASHHOLDER.getField());
                if(o==null) {
                    return null;
                }
                return PacketUtil.bukkitEntityFromNMS(o);
            }
            val h = ((Mob)entity).getLeashHolder();
            if(h==null) return null;
            return h.getBukkitEntity();
        } catch (Throwable t){
            t.printStackTrace();
            return null;
        }
    }

    @SneakyThrows
    private void createEntity(World world){
        entity = (net.minecraft.world.entity.Entity) ReflectionUtil.constructObject(Class.forName(entityClass), ((Optional<?>)Class.forName("net.minecraft.world.entity.EntityTypes").getMethod(FieldMappings.ENTITYTYPE_BYSTRING.getField(), String.class).invoke(null, entityTypeEnum)).get(), PacketUtil.getNMSWorld(world));
    }

    public void changeWorld(World world){
        receivers.forEach(this::despawn);
        receivers.clear();
        entity.changeDimension((ServerLevel) PacketUtil.getNMSWorld(world));
    }

    @SneakyThrows
    public PacketEntity(String entityClass, String entityTypeEnum, World world){
        this.entityClass = entityClass;
        this.entityTypeEnum = entityTypeEnum;
        createEntity(world);
        spawn();
    }

    @SneakyThrows
    public World getWorld(){
        if(compatibility) {
            return Bukkit.getWorld((UUID) ReflectionUtil.runMethod(ReflectionUtil.runMethod(level(), "getWorld"), "getUID"));
        }
        return Bukkit.getWorld(entity.level().getWorld().getUID());
    }
    public Location getLocation(){
        return new Location(getWorld(), locX(), locY(), locZ());
    }

    @SneakyThrows
    private Level level(){
        if(compatibility) return (Level) entity.getClass().getMethod(FieldMappings.ENTITY_GETLEVEL.getField()).invoke(entity);
        return entity.level();
    }

    @SneakyThrows
    public double locX(){
        if(compatibility) return (double) ReflectionUtil.runMethod(entity, FieldMappings.ENTITY_LOCX.getField());
        return entity.getX();
    }

    @SneakyThrows
    public double locY(){
        if(compatibility) return (double) ReflectionUtil.runMethod(entity, FieldMappings.ENTITY_LOCY.getField());
        return entity.getY();
    }

    @SneakyThrows
    public double locZ(){
        if(compatibility) return (double) ReflectionUtil.runMethod(entity, FieldMappings.ENTITY_LOCZ.getField());
        return entity.getZ();
    }

    @SneakyThrows
    public void persistentInvisibility(boolean value){
//        if(compatibility) ReflectionUtil.setField(entity, "persistentInvisibility", visible);
//        entity.persistentInvisibility = value;
        entity.persistentInvisibility = value;
        if(compatibility){
            ReflectionUtil.runMethod(entity, FieldMappings.ENTITY_SETSHAREDFLAG.getField(), 5, value);
        } else {
            entity.setSharedFlag(5, value);
        }
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
        if(!l.getWorld().isChunkLoaded(chunkX, chunkZ)) {
            return new ArrayList<>();
        }
        List<Entity> list = new ArrayList<>();
        for (int x = -radiusInChunks; x <= radiusInChunks; x++) {
            for (int z = -radiusInChunks; z <= radiusInChunks; z++) {
                int finalX = x+chunkX;
                int finalZ = z+chunkZ;
                if(!l.getWorld().isChunkLoaded(finalX, finalZ)) continue;
                try {
                    Chunk c = l.getWorld().getChunkAt(finalX,finalZ);
                    list.addAll(Arrays.asList(c.getEntities()));
                } catch (Throwable t){
//                    t.printStackTrace();
                }
            }
        }
        return list;
    }
    public List<Entity> getNearbyEntities(int radiusInChunks, EntityType ofType){
        return getNearbyEntities(radiusInChunks).stream().filter(entity1 -> entity1.getType()==ofType).toList();
    }

    public void teleport(Location l){
        teleport(l.getWorld(), l.getX(), l.getY(), l.getZ());
    }

    @SneakyThrows
    public void teleport(double x, double y, double z) {
        if(compatibility){
            ReflectionUtil.runMethod(entity, FieldMappings.ENTITY_SET_POS.getField(),x, y, z);
        } else {
            entity.teleportTo(x,y,z);
        }
        if(isSpawned) receivers.forEach(this::sendTeleportPacket);
    }
    @SneakyThrows
    public void teleport(double x, double y, double z, float yaw, float pitch) {
        if(compatibility){
            ReflectionUtil.runMethod(entity, FieldMappings.ENTITY_TELEPORT_TO_WITH_FLAGS.getField(), level(), x, y, z, new HashSet<>(), yaw, pitch);
        } else {
            entity.teleportTo((ServerLevel) level(), x,y,z, new HashSet<>(), yaw, pitch);
        }
        if(isSpawned) receivers.forEach(this::sendTeleportPacket);
    }
    @SneakyThrows
    public void teleport(World w, double x, double y, double z, float yaw, float pitch) {
        if(compatibility){
            ReflectionUtil.runMethod(entity, FieldMappings.ENTITY_TELEPORT_TO_WITH_FLAGS.getField(), PacketUtil.getNMSWorld(w), x, y, z, new HashSet<>(),  yaw, pitch);
        } else {
            entity.teleportTo((ServerLevel) PacketUtil.getNMSWorld(w), x,y,z, new HashSet<>(), yaw, pitch);
        }
        if(isSpawned) receivers.forEach(this::sendTeleportPacket);
    }
    @SneakyThrows
    public void teleport(World w, double x, double y, double z) {
        World oldWorld = getWorld();
        if(oldWorld==null || !oldWorld.getUID().equals(w.getUID())) changeWorld(w);
        teleport(x,y,z);
//        if(compatibility){
//            ReflectionUtil.runMethod(entity, FieldMappings.ENTITY_SET_POS.getField(),x, y, z);
//        } else {
//            ((net.minecraft.world.entity.Entity)entity).teleportTo(x,y,z);
//        }
//        if(isSpawned) receivers.forEach(this::sendTeleportPacket);
    }
    @SneakyThrows
    public T nameVisible(boolean value) {
        if(compatibility) ReflectionUtil.runMethod(entity, FieldMappings.ENTITY_SETCUSTOMNAMEVISIBLE.getField(),value); else entity.setCustomNameVisible(value);
        return (T) this;
    }
    @SneakyThrows
    public T displayName(String name){
        if(compatibility) ReflectionUtil.runMethod(entity, FieldMappings.ENTITY_SETCUSTOMNAME.getField(), Component.literal(Commons.colorizeWithHex(name))); else entity.setCustomName(Component.literal(Commons.colorizeWithHex(name)));
        if(isSpawned) metadata();
        return (T) this;
    }
    public T tick(boolean value){
        this.isTicking = value;
        return (T) this;
    }

    public T receivers(List<Player> receivers){
        receivers.removeIf(excludedReceivers::contains);
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
        if(excludedReceivers.contains(p)) return (T) this;
        this.receivers.add(p);
        if(isSpawned) spawn(p);
        return (T) this;
    }

    public T exclude(Player p){
        if(excludedReceivers.contains(p)) return (T) this;
        excludedReceivers.add(p);
        return (T) this;
    }

    public T unexclude(Player p){
        if(!excludedReceivers.contains(p)) return (T) this;
        excludedReceivers.remove(p);
        return (T) this;
    }

    public List<Player> excluded(){
        return excludedReceivers;
    }
    // ONLY if it is EntityLiving
    @SneakyThrows
    public T collides(boolean value) {
        if(entity instanceof LivingEntity le) {
            le.collides = value;
        }
//        ReflectionUtil.setField(entity, "collides", value);
        return (T) this;
    }

    public void spawn(){
        receivers.forEach(this::spawn);
        isSpawned = true;
    }

    public void spawn(Player player){
        sendSpawnPacket(player);
        sendMetadataPacket(player);
        if(equipment!=null && equipment.size()!=0) sendEquipmentPacket(player);
        if(leashHolderSet) {
            Entity leashHolder = leashHolder();
            sendLeashPacket(player, leashHolder);
        }
    }

    public void sendLeashPacket(Player p, Entity leashHolder){
        PacketUtil.sendLeashPacket(p, leashHolder, PacketUtil.bukkitEntityFromNMS(entity));
    }

    public void metadata(){
        metadata(true);
    }
    public void metadata(boolean full){
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
        receivers.clear();
        isSpawned = false;
    }

    public void despawn(Player player){
        sendDespawnPacket(player);
    }

    protected void sendSpawnPacket(Player p){
        PacketUtil.spawnLivingEntity(p, entity);
    }
    protected void sendMetadataPacket(Player p){
        sendMetadataPacket(p, true);
    }
    protected void sendMetadataPacket(Player p, boolean full){
        PacketUtil.entityMetadata(p, entity, full);
    }
    protected void sendEquipmentPacket(Player p){
        PacketUtil.equipEntity(p, entity, equipment);
    }
    protected void sendTeleportPacket(Player p){
        PacketUtil.teleportEntity(p, entity);
    }
    protected void sendDespawnPacket(Player p){
        PacketUtil.destroyEntity(p, entity);
    }

    protected int visibilityTickCounter = 0;
    public void tick(){
        if(showInRadius==-1) return;
        visibilityTickCounter++;
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
