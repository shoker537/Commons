package ru.shk.commons.utils.nms;


import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.SneakyThrows;
import lombok.val;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.MaterialMapColor;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import ru.shk.commons.Commons;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public abstract class Version {


    protected abstract void sendPacket(Player p, Packet<?> packet) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException;

    protected abstract Packet<?> createScoreboardTeamPacket(String name, String prefix, String suffix) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException;
    protected abstract Packet<?> createSetBlockPacket(Block block);

    protected abstract String getItemTypeTranslationKey(Material m);
    protected abstract ItemStack asNMSCopy(org.bukkit.inventory.ItemStack itemStack);
    protected abstract Object getNMSWorld(World world);
    protected abstract void explodeFirework(Player p, Location l, org.bukkit.inventory.ItemStack firework);

    @SneakyThrows
    protected void explodeFirework(Player p, Location l, org.bukkit.inventory.ItemStack firework, String dataWatcherField, String idField){
        p.playSound(l, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1);
        Object fw = ConstructorUtils.invokeConstructor(Class.forName("net.minecraft.world.entity.projectile.EntityFireworks"), new Object[]{getNMSWorld(p.getWorld()), l.getX(), l.getY(), l.getZ(), asNMSCopy(firework)});
        Object dataWatcher = fw.getClass().getMethod(dataWatcherField).invoke(fw);
        Object id = fw.getClass().getMethod(idField).invoke(fw);
        PacketUtil.sendPacket(p, (Packet<?>) ConstructorUtils.invokeConstructor(Class.forName("net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity"), new Object[]{fw, 76}));
        PacketUtil.sendPacket(p, (Packet<?>) ConstructorUtils.invokeConstructor(Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata"), new Object[]{id, dataWatcher, true}));
        PacketUtil.sendPacket(p, (Packet<?>) ConstructorUtils.invokeConstructor(Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityStatus"), new Object[]{fw, (byte)17}));
        PacketUtil.sendPacket(p, (Packet<?>) ConstructorUtils.invokeConstructor(Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy"), new Object[]{IntList.of((Integer) id)}));
    }

    @SneakyThrows
    protected void spawnLivingEntity(Player p, Object e){
        Class<?> packet = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLiving");
        Packet<?> pk = (Packet<?>) ConstructorUtils.invokeConstructor(packet, new Object[]{e});
        sendPacket(p, pk);
    }
    @SneakyThrows
    protected void spawnEntity(Player p, Object e){
        Class<?> packet = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity");
        Packet<?> pk = (Packet<?>) ConstructorUtils.invokeConstructor(packet, new Object[]{e});
        sendPacket(p, pk);
    }
    @SneakyThrows
    protected void destroyEntity(Player p, Object e){
        Class<?> packet = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy");
        int id = (int) e.getClass().getMethod(FieldMappings.ENTITY_GETID.getField()).invoke(e);
        Packet<?> pk = (Packet<?>) ConstructorUtils.invokeConstructor(packet, new Object[]{IntList.of(id)});
        sendPacket(p, pk);
    }

    @SneakyThrows
    protected void equipEntity(Player p, Object e, List<Pair<EnumItemSlot, ItemStack>> items){
        Class<?> packet = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityEquipment");
        int id = (int) e.getClass().getMethod(FieldMappings.ENTITY_GETID.getField()).invoke(e);
        Packet<?> pk = (Packet<?>) ConstructorUtils.invokeConstructor(packet, new Object[]{id, items});
        sendPacket(p, pk);
    }

    @SneakyThrows
    protected void entityMetadata(Player p, Object e){
        Class<?> packet = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata");
        Object dataWatcher = e.getClass().getMethod(FieldMappings.ENTITY_GETDATAWATCHER.getField()).invoke(e);
        int id = (int) e.getClass().getMethod(FieldMappings.ENTITY_GETID.getField()).invoke(e);
        Packet<?> pk = (Packet<?>) ConstructorUtils.invokeConstructor(packet, new Object[]{id, dataWatcher, true});
        sendPacket(p, pk);
    }

    @SneakyThrows
    protected void teleportEntity(Player p, Object e){
        Class<?> packet = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityTeleport");
        Packet<?> pk = (Packet<?>) ConstructorUtils.invokeConstructor(packet, new Object[]{e});
        sendPacket(p, pk);
    }

    @SneakyThrows
    protected void playTotemAnimation(Player p){
        Class<?> statusPacket = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityStatus");
        val a = (Packet<?>) ConstructorUtils.invokeConstructor(statusPacket, new Object[]{getNMSPlayer(p), (byte)35});
        sendPacket(p, a);
    }
    @SneakyThrows
    protected EntityPlayer getNMSPlayer(Player p){
        return (EntityPlayer) p.getClass().getMethod("getHandle").invoke(p);
    }

    public abstract net.minecraft.world.level.block.Block getBlock(Material m);

    @SneakyThrows
    protected int getMaterialColorInt(Material m){
        return getBlock(m).s().al;
    }

}
