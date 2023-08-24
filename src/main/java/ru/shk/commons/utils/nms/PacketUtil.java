package ru.shk.commons.utils.nms;

import com.mojang.datafixers.util.Pair;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.Packet;
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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class PacketUtil {
    public static List<PacketEntity<?>> entitiesToTick = new ArrayList<>();
    private static final ThreadPoolExecutor asyncEntityTicker = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);

    private static final Version versionClass;
    static {
        Version versionClass1;
        try {
            Class<? extends Version> cl = (Class<? extends Version>) Class.forName("ru.shk.commons.utils.nms.version."+Commons.getServerVersion());
            versionClass1 = cl.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            versionClass1 = null;
        }
        versionClass = versionClass1;

        Commons.getInstance().asyncRepeating(() -> {
            if(entitiesToTick.size()==0 || asyncEntityTicker.getQueue().size()!=0) return;
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
    *  @param createTeamOrUpdate true - create, false - update
    */
    public static void createAndSendTeam(boolean createTeamOrUpdate, String name, String prefix, String suffix, ChatColor color, List<String> entries, Player... toSend) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException {
        for (Player player : toSend) sendPacket(player, versionClass.createScoreboardTeamPacket(createTeamOrUpdate, true,name, prefix, suffix, color, entries));
    }

    /**
     *  @param createTeamOrUpdate true - create, false - update
     */
    public static void createAndSendTeam(boolean createTeamOrUpdate, boolean collideTeammates, String name, String prefix, String suffix, ChatColor color, List<String> entries, Player... toSend) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException {
        for (Player player : toSend) sendPacket(player, versionClass.createScoreboardTeamPacket(createTeamOrUpdate,collideTeammates,name, prefix, suffix, color, entries));
    }
    /**
     *  @param createTeamOrUpdate true - create, false - update
     */
    public static void createAndSendTeam(boolean createTeamOrUpdate, boolean collideTeammates, boolean friendlyFire, boolean canSeeFriendlyInvisible, String name, String prefix, String suffix, ChatColor color, List<String> entries, Player... toSend) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException {
        for (Player player : toSend) sendPacket(player, versionClass.createScoreboardTeamPacket(createTeamOrUpdate,collideTeammates,friendlyFire, canSeeFriendlyInvisible, name, prefix, suffix, color, entries));
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

    public static void sendPacket(Player p, Packet<?>... packets) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        for (Packet<?> packet : packets) versionClass.sendPacket(p, packet);
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

    public static void entityMetadata(Player p, Object entity){
        entityMetadata(p, entity, true);
    }
    public static void entityMetadata(Player p, Object entity, boolean full){
        versionClass.entityMetadata(p, entity, full);
    }
    public static void teleportEntity(Player p, Object entity){
        versionClass.teleportEntity(p, entity);
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
