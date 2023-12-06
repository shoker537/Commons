package ru.shk.commons.utils.nms;

import com.mojang.datafixers.util.Pair;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ru.shk.commons.Commons;
import ru.shk.commons.utils.nms.entity.PacketEntity;

import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class PacketUtil {
    public static List<PacketEntity<?>> entitiesToTick = new ArrayList<>();
    private static final ThreadPoolExecutor asyncEntityTicker = (ThreadPoolExecutor) Executors.newFixedThreadPool(3, new DefaultThreadFactory("Commons Entity Ticking Pool"));

    private static final CurrentVersion versionClass = new CurrentVersion();
    static {

        Commons.getInstance().asyncRepeating(() -> {
            if(entitiesToTick.isEmpty() || !asyncEntityTicker.getQueue().isEmpty()) return;
            entitiesToTick.removeIf(packetEntity -> !packetEntity.isValid());
            entitiesToTick.forEach(packetEntity -> {
                if(packetEntity.isTicking()) asyncEntityTicker.submit(() -> {
                    try {
                         packetEntity.tick();
                    } catch (Throwable t){
                        t.printStackTrace();
                    }
                });
            });
        }, 1,1);
    }

    public static Object getNMSEntity(Entity e){
        return versionClass.getNMSEntity(e);
    }

    /**
    *  @param createTeam true - create, false - update
    */
    public static void createAndSendTeam(boolean createTeam, String name, String prefix, String suffix, ChatColor color, List<String> entries, Player... toSend) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException {
        for (Player player : toSend) sendPacket(player, versionClass.createScoreboardTeamPacket(createTeam, true,name, prefix, suffix, color, entries));
    }
    public static Object createMapPacket(int mapId, BufferedImage image){
        return versionClass.createMapPacket(mapId, image);
    }
    public static void sendMap(List<Player> players, int mapId, BufferedImage image){
        versionClass.sendMap(players, mapId, image);
    }
    public static void sendMap(Player player, int mapId, BufferedImage image){
        sendMap(List.of(player), mapId, image);
    }

    /**
     *  @param createTeam true - create, false - update
     */
    public static void createAndSendTeam(boolean createTeam, boolean collideTeammates, String name, String prefix, String suffix, ChatColor color, List<String> entries, Player... toSend) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException {
        for (Player player : toSend) sendPacket(player, versionClass.createScoreboardTeamPacket(createTeam,collideTeammates,name, prefix, suffix, color, entries));
    }
    /**
     *  @param createTeam true - create, false - update
     */
    public static void createAndSendTeam(boolean createTeam, boolean collideTeammates, boolean friendlyFire, boolean canSeeFriendlyInvisible, String name, String prefix, String suffix, ChatColor color, List<String> entries, Player... toSend) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException {
        for (Player player : toSend) sendPacket(player, versionClass.createScoreboardTeamPacket(createTeam,collideTeammates,friendlyFire, canSeeFriendlyInvisible, name, prefix, suffix, color, entries));
    }

    public static void sendLeashPacket(Player p, Entity owner, Entity attached){
        versionClass.leashPacket(p, owner, attached);
    }

    public static Entity bukkitEntityFromNMS(Object entity){
        return versionClass.bukkitEntityFromNMS(entity);
    }

    public static void chestOpenState(Location l, boolean open){
        versionClass.chestOpenState(l, open);
    }

    public static void removeTeamPacket(String team, Player... toSend) throws NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        for (Player player : toSend) sendPacket(player, versionClass.createRemoveTeamPacket(team));
    }

    public static int getMaterialColorInt(Material m){
        return versionClass.getMaterialColorInt(m);
    }

    public static void sendPacket(Player p, Object... packets) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        for (Object packet : packets) versionClass.sendPacket(p, (Packet<?>) packet);
    }
    public static void sendPacket(List<Player> players, Object... packets) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        for (Object packet : packets) {
            for (Player p : players) {
                versionClass.sendPacket(p, (Packet<?>) packet);
            }
        }
    }

    public static void sendAddPlayerProfile(Player p, Object serverPlayer){
        versionClass.addPlayerProfile(p, (ServerPlayer) serverPlayer);
    }

    public static void sendRemovePlayerProfiles(Player p, List<UUID> toRemove){
        versionClass.removePlayerProfiles(p, toRemove);
    }

    public static void sendScoreboardTeamPacket(Player p, String name, String prefix, String suffix) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
        Packet<?> packet = versionClass.createScoreboardTeamPacket(false,name, prefix, suffix);
        sendPacket(p, packet);
    }
    public static void sendScoreboardTeamPacket(boolean createTeamOrUpdate, Player p, String name, String prefix, String suffix) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
        Packet<?> packet = versionClass.createScoreboardTeamPacket(createTeamOrUpdate,name, prefix, suffix);
        sendPacket(p, packet);
    }
    public static void sendScoreboardTeamPacket(boolean createTeamOrUpdate, Player p, String name, Component prefix, Component suffix) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
        Packet<?> packet = versionClass.createScoreboardTeamPacket(createTeamOrUpdate,name, prefix, suffix);
        sendPacket(p, packet);
    }

    public static Object getNMSWorld(World world) {
        if (world == null) return null;
        return versionClass.getNMSWorld(world);
    }
    public static ItemStack asNMSCopy(org.bukkit.inventory.ItemStack item) {
        if (item == null) return null;
        return versionClass.asNMSCopy(item);
    }
    public static String getItemTypeTranslationKey(Material material) {
        if (material == null) return null;
        return versionClass.getItemTypeTranslationKey(material);
    }
    public static void explodeFirework(Player p, Location l, org.bukkit.inventory.ItemStack firework){
        versionClass.explodeFirework(p, l, firework);
    }

    public static void spawnLivingEntity(Player p, Object entity){
        versionClass.spawnLivingEntity(p, entity);
    }
    public static void spawnEntity(Player p, Object entity){
        versionClass.spawnEntity(p, entity);
    }
    public static void spawnPlayer(Player p, Object player){
        versionClass.spawnPlayer(p, player);
    }
    public static void sendEntityAnimation(Player p, Object entity, int animationId){
        versionClass.entityAnimation(p, (net.minecraft.world.entity.Entity) entity, animationId);
    }

    public static void entityMetadata(Player p, Object entity){
        entityMetadata(p, entity, true);
    }
    public static void entityMetadata(Player p, Object entity, boolean full){
        versionClass.entityMetadata(p, entity, full);
    }
    public static void teleportEntity(Player p, Object entity){
        versionClass.teleportEntity(p, (net.minecraft.world.entity.Entity) entity);
    }
    public static void teleportPlayer(Player p, double x, double y, double z, float yaw, float pitch, int teleportId){

    }
    public static void destroyEntity(Player p, Object entity){
        versionClass.destroyEntity(p, entity);
    }

    public static void equipEntity(Player p, Object entity, HashMap<ItemSlot, org.bukkit.inventory.ItemStack> items){
        List<Pair<EquipmentSlot, ItemStack>> list = new ArrayList<>();
        if(items==null) return;
        items.forEach((slot, itemStack) -> list.add(new Pair<>(slot.getNmsSlot(), asNMSCopy(itemStack))));
        versionClass.equipEntity(p, entity, list);
    }

    public static void playRiptideAnimation(Player p, int ticks){
        versionClass.playRiptideAnimation(p, ticks);
    }

    public static void playTotemAnimation(Player p){
        versionClass.playTotemAnimation(p);
    }
}
