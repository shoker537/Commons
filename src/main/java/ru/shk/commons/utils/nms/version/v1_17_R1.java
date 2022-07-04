package ru.shk.commons.utils.nms.version;

import lombok.SneakyThrows;
import lombok.val;
import net.minecraft.EnumChatFormat;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardTeam;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_17_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import ru.shk.commons.utils.nms.Version;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class v1_17_R1 extends Version {
    @Override
    public void sendPacket(Player p, Packet<?> packet) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        Method getHandle = p.getClass().getMethod("getHandle");
        Object nmsPlayer = getHandle.invoke(p);
        Field con_field = nmsPlayer.getClass().getField("b");
        Object con = con_field.get(nmsPlayer);
        Method packet_method = con.getClass().getMethod("sendPacket", Packet.class);
        packet_method.invoke(con, packet);
    }

    @Override
    public Packet<?> createScoreboardTeamPacket(boolean createTeamOrUpdate, String name, String prefix, String suffix) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return createScoreboardTeamPacket(createTeamOrUpdate,name, prefix, suffix, null, null);
    }

    @Override
    protected Packet<?> createScoreboardTeamPacket(boolean createTeamOrUpdate, String name, String prefix, String suffix, ChatColor color, List<String> entries) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ScoreboardTeam t = new ScoreboardTeam(new Scoreboard(), name);
        t.setPrefix(IChatBaseComponent.a(prefix));
        t.setSuffix(IChatBaseComponent.a(suffix));
        if(color!=null) t.setColor(EnumChatFormat.valueOf(color.name()));
        if(entries!=null) t.getPlayerNameSet().addAll(entries);
        return PacketPlayOutScoreboardTeam.a(t, createTeamOrUpdate);
    }

    @Override
    public Packet<?> createSetBlockPacket(Block block) {
        return null;
    }

    @Override
    protected Packet<?> createRemoveTeamPacket(String team) {
        return PacketPlayOutScoreboardTeam.a(new ScoreboardTeam(new Scoreboard(), team));
    }

    @Override
    public String getItemTypeTranslationKey(Material m) {
        Item nmsItem = CraftMagicNumbers.getItem(m);
        if (nmsItem == null) return null;
        return nmsItem.getName();
    }

    @Override
    public ItemStack asNMSCopy(org.bukkit.inventory.ItemStack itemStack) {
        return CraftItemStack.asNMSCopy(itemStack);
    }

    @Override@SneakyThrows
    public Object getNMSWorld(World world) {
        Class<?> wClass = Class.forName("net.minecraft.world.level.World");
        Class<?> c = Class.forName("org.bukkit.craftbukkit.v1_17_R1.CraftWorld");
        val craftWorld = c.cast(world);
        return wClass.cast(craftWorld.getClass().getMethod("getHandle").invoke(craftWorld));
    }

    @Override
    protected void explodeFirework(Player p, Location l, org.bukkit.inventory.ItemStack firework) {
        explodeFirework(p, l, firework, "ad", "Z");
    }

    @Override@SneakyThrows
    public net.minecraft.world.level.block.Block getBlock(Material m) {
        Class<?> c = Class.forName("org.bukkit.craftbukkit.v1_17_R1.util.CraftMagicNumbers");
        return (net.minecraft.world.level.block.Block) c.getMethod("getBlock", Material.class).invoke(null, m);
    }

}
