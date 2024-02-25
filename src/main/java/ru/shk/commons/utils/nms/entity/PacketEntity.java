package ru.shk.commons.utils.nms.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.val;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.shk.commons.Commons;
import ru.shk.commons.utils.nms.ItemSlot;
import ru.shk.commons.utils.nms.PacketUtil;
import ru.shk.commons.utils.nms.ReflectionUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

@SuppressWarnings({"unused", "unchecked"})
public class PacketEntity<T extends PacketEntity> {
    protected final List<Player> receivers = new CopyOnWriteArrayList<>();
    protected final List<Player> excludedReceivers = new CopyOnWriteArrayList<>();
    private final String entityClass;
    private final String entityTypeEnum;
    @Getter protected net.minecraft.world.entity.Entity entity;
    @Getter protected HashMap<ItemSlot, ItemStack> equipment;
    protected boolean isSpawned = false;
    @Getter private boolean isTicking = false;
    @Getter@Setter private int showInRadius = -1;
    @Getter@Setter private boolean isValid = true;
    private boolean leashHolderSet = false;

    @SneakyThrows
    public PacketEntity(String entityClass, String entityTypeId, World world, double x, double y, double z){
        this(entityClass, entityTypeId, world);
        teleport(x,y,z);
    }

    public PacketEntity(String entityClass, String entityTypeId, Location l){
        this(entityClass, entityTypeId, l.getWorld(), l.getX(), l.getY(), l.getZ());
    }

    @SneakyThrows
    public synchronized void leashHolder(Entity e){
        leashHolderSet = e!=null;
        ((Mob)entity).setLeashedTo((net.minecraft.world.entity.Entity) PacketUtil.getNMSEntity(e), true);
    }

    @SneakyThrows
    public Entity leashHolder(){
        try {
            val h = ((Mob)entity).getLeashHolder();
            if(h==null) return null;
            return h.getBukkitEntity();
        } catch (Throwable t){
            t.printStackTrace();
            return null;
        }
    }

    @SneakyThrows
    public void createEntity(World world){
        entity = (net.minecraft.world.entity.Entity) ReflectionUtil.constructObject(Class.forName(entityClass), net.minecraft.world.entity.EntityType.byString(entityTypeEnum).get(), PacketUtil.getNMSWorld(world));
    }

    public synchronized void changeWorld(World world){
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
        return entity.level().getWorld();
    }
    public Location getLocation(){
        return new Location(getWorld(), locX(), locY(), locZ());
    }

    @SneakyThrows
    private Level level(){
        return entity.level();
    }

    @SneakyThrows
    public double locX(){
        return entity.getX();
    }

    @SneakyThrows
    public double locY(){
        return entity.getY();
    }

    @SneakyThrows
    public double locZ(){
        return entity.getZ();
    }

    @SneakyThrows
    public synchronized void persistentInvisibility(boolean value){
        entity.persistentInvisibility = value;
        entity.setSharedFlag(5, value);
    }

    public void pose(org.bukkit.entity.Pose pose){
        pose(pose, true);
    }
    public void pose(org.bukkit.entity.Pose pose, boolean sendMetadata){
        entity.setPose(asNMSPose(pose));
        if(sendMetadata) metadata();
    }

    private Pose asNMSPose(org.bukkit.entity.Pose pose){
        return switch (pose){
            case SNEAKING -> Pose.CROUCHING;
            default -> Pose.valueOf(pose.name());
        };
    }

    public T equip(ItemSlot slot, ItemStack item){
        if(equipment==null) equipment = new HashMap<>();
        equipment.put(slot, item);
        if(isSpawned) equipment();
        return (T) this;
    }

    public List<Entity> getNearbyEntities(int radiusInChunks){
        Location l = getLocation();
        try {
            Collection<Entity> c = CompletableFuture.supplyAsync(() -> l.getNearbyEntities(radiusInChunks*16, radiusInChunks*16, radiusInChunks*16), Bukkit.getScheduler().getMainThreadExecutor(Commons.getInstance())).get();
            return new ArrayList<>(c);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
    public List<Entity> getNearbyEntities(int radiusInChunks, EntityType ofType){
        return getNearbyEntities(radiusInChunks).stream().filter(entity1 -> entity1.getType()==ofType).toList();
    }

    public synchronized void teleport(Location l){
        teleport(l, true);
    }
    public synchronized void teleport(Location l, boolean sendPackets){
        teleport(l.getWorld(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), sendPackets);
    }

    @SneakyThrows
    public synchronized void teleport(double x, double y, double z, boolean sendPacket) {
        entity.moveTo(x,y,z);
        if(sendPacket && isSpawned) receivers.forEach(this::sendTeleportPacket);
    }

    @SneakyThrows
    public synchronized void teleport(double x, double y, double z) {
        teleport(x,y,z, true);
    }
    @SneakyThrows
    public synchronized void teleport(double x, double y, double z, float yaw, float pitch, boolean sendPackets) {
        teleport(getWorld(), x,y,z,yaw, pitch, sendPackets);
    }
    @SneakyThrows
    public synchronized void teleport(double x, double y, double z, float yaw, float pitch) {
        teleport(x,y,z,yaw, pitch, true);
    }
    @SneakyThrows
    public synchronized void teleport(World w, double x, double y, double z, float yaw, float pitch, boolean sendPackets) {
        if(!w.getUID().equals(getWorld().getUID())) entity.changeDimension((ServerLevel) PacketUtil.getNMSWorld(w));
        entity.moveTo(x,y,z, yaw, pitch);
        if(sendPackets && isSpawned) receivers.forEach(this::sendTeleportPacket);
    }
    @SneakyThrows
    public synchronized void teleport(World w, double x, double y, double z, float yaw, float pitch) {
        teleport(w,x,y,z,yaw, pitch, true);
    }
    @SneakyThrows
    public synchronized void teleport(World w, double x, double y, double z, boolean sendPackets) {
        World oldWorld = getWorld();
        if(oldWorld==null || !oldWorld.getUID().equals(w.getUID())) changeWorld(w);
        teleport(x,y,z, sendPackets);
    }
    @SneakyThrows
    public synchronized void teleport(World w, double x, double y, double z) {
        teleport(w,x,y,z, true);
    }
    @SneakyThrows
    public T nameVisible(boolean value) {
        entity.setCustomNameVisible(value);
        return (T) this;
    }
    @SneakyThrows
    public T displayName(String name){
        entity.setCustomName(Component.literal(Commons.colorizeWithHex(name)));
        if(isSpawned) metadata();
        return (T) this;
    }
    public T tick(boolean value){
        this.isTicking = value;
        return (T) this;
    }

    @SneakyThrows
    public T glowing(boolean value){
        entity.setGlowingTag(value);
        if(isSpawned) metadata();
        return (T) this;
    }

    public boolean glowing(){
        return entity.isCurrentlyGlowing();
    }

    public T receivers(List<Player> receivers){
        receivers.removeIf(excludedReceivers::contains);
        this.receivers.clear();
        this.receivers.addAll(receivers);
        if(isSpawned) receivers.forEach(this::spawn);
        return (T) this;
    }

    public synchronized void removeReceiver(Player p){
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

    public synchronized void spawn(){
        if(entity==null) return;
        receivers.forEach(this::spawn);
        isSpawned = true;
    }

    public synchronized void spawn(Player player){
        sendSpawnPacket(player);
        sendMetadataPacket(player);
        if(equipment!=null && !equipment.isEmpty()) sendEquipmentPacket(player);
        if(leashHolderSet) {
            Entity leashHolder = leashHolder();
            sendLeashPacket(player, leashHolder);
        }
    }

    public synchronized void sendLeashPacket(Player p, Entity leashHolder){
        PacketUtil.sendLeashPacket(p, leashHolder, PacketUtil.bukkitEntityFromNMS(entity));
    }

    public synchronized void metadata(){
        metadata(true);
    }
    public synchronized void metadata(boolean full){
        receivers.forEach(this::sendMetadataPacket);
    }
    public synchronized void equipment(){
        receivers.forEach(this::sendEquipmentPacket);
    }
    public synchronized void equipment(Player p){
        sendEquipmentPacket(p);
    }

    public synchronized void metadata(Player player){
        sendMetadataPacket(player);
    }

    public synchronized void despawn(){
        receivers.forEach(this::sendDespawnPacket);
        receivers.clear();
        isSpawned = false;
    }

    public synchronized void despawn(Player player){
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
    public synchronized void tick(){
        if(showInRadius==-1) return;
        visibilityTickCounter++;
        if(visibilityTickCounter==20) {
            visibilityTickCounter = 0;
            visibilityTick();
        }
    }

    public synchronized void visibilityTick(){
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
        return entity.getEntityData();
    }

}
