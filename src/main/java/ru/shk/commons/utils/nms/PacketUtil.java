package ru.shk.commons.utils.nms;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.shk.commons.Commons;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PacketUtil {

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
    }

    public static int getMaterialColorInt(Material m){
        return versionClass.getMaterialColorInt(m);
    }

    public static void sendPacket(Player p, Packet<?>... packets) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        for (Packet<?> packet : packets) versionClass.sendPacket(p, packet);
    }

    public static void sendScoreboardTeamPacket(Player p, String name, String prefix, String suffix) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
        Packet<?> packet = versionClass.createScoreboardTeamPacket(name, prefix, suffix);
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
        versionClass.entityMetadata(p, entity);
    }
    public static void teleportEntity(Player p, Object entity){
        versionClass.teleportEntity(p, entity);
    }
    public static void destroyEntity(Player p, Object entity){
        versionClass.destroyEntity(p, entity);
    }

    public static void equipEntity(Player p, Object entity, HashMap<NMSItemSlot, org.bukkit.inventory.ItemStack> items){
        List<Pair<EnumItemSlot, ItemStack>> list = new ArrayList<>();
        if(items==null) return;
//        if(!items.containsKey(NMSItemSlot.HEAD)) items.put(NMSItemSlot.HEAD, new org.bukkit.inventory.ItemStack(Material.AIR));
//        if(!items.containsKey(NMSItemSlot.CHEST)) items.put(NMSItemSlot.CHEST, new org.bukkit.inventory.ItemStack(Material.AIR));
//        if(!items.containsKey(NMSItemSlot.LEGGINGS)) items.put(NMSItemSlot.LEGGINGS, new org.bukkit.inventory.ItemStack(Material.AIR));
//        if(!items.containsKey(NMSItemSlot.BOOTS)) items.put(NMSItemSlot.BOOTS, new org.bukkit.inventory.ItemStack(Material.AIR));
        items.forEach((slot, itemStack) -> list.add(new Pair<>(asEnumItemSlot(slot), asNMSCopy(itemStack))));
        versionClass.equipEntity(p, entity, list);
    }

    private static EnumItemSlot asEnumItemSlot(NMSItemSlot slot){
        return switch (slot){
            case HEAD -> EnumItemSlot.f;
            case CHEST -> EnumItemSlot.e;
            case LEGGINGS -> EnumItemSlot.d;
            case BOOTS -> EnumItemSlot.c;
        };
    }

    public static void playTotemAnimation(Player p){
        versionClass.playTotemAnimation(p);
    }
}
