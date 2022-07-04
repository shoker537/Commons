package ru.shk.commons.utils.nms.version;

import lombok.SneakyThrows;
import lombok.val;
import net.minecraft.EnumChatFormat;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.ScoreboardTeam;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import ru.shk.commons.utils.nms.Version;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

public class v1_19_R1 extends Version {
    @Override
    public void sendPacket(Player p, Packet<?> packet) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        Method getHandle = p.getClass().getMethod("getHandle");
        Object nmsPlayer = getHandle.invoke(p);
        Field con_field = nmsPlayer.getClass().getField("b");
        Object con = con_field.get(nmsPlayer);
        Method packet_method = con.getClass().getMethod("a", Packet.class);
        packet_method.invoke(con, packet);
    }

    @Override
    public Packet<?> createScoreboardTeamPacket(boolean createTeamOrUpdate, String name, String prefix, String suffix) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return createScoreboardTeamPacket(createTeamOrUpdate,name, prefix, suffix, null, null);
    }

    @Override
    protected Packet<?> createScoreboardTeamPacket(boolean createTeamOrUpdate, String name, String prefix, String suffix, ChatColor color, List<String> entries) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ScoreboardTeam t = new ScoreboardTeam(new net.minecraft.world.scores.Scoreboard(), name);
        t.getClass().getMethod("b", IChatBaseComponent.class).invoke(t, IChatBaseComponent.a(prefix));
        t.getClass().getMethod("c", IChatBaseComponent.class).invoke(t, IChatBaseComponent.a(suffix));
        if(color!=null) t.getClass().getMethod("a", EnumChatFormat.class).invoke(t, EnumChatFormat.valueOf(color.name()));
        if(entries!=null){
            Collection<String> e = (Collection<String>) t.getClass().getMethod("g").invoke(t);
            e.addAll(entries);
        }
        return PacketPlayOutScoreboardTeam.a(t, createTeamOrUpdate);
    }

    @Override
    protected Packet<?> createRemoveTeamPacket(String team) {
        ScoreboardTeam t = new ScoreboardTeam(new net.minecraft.world.scores.Scoreboard(), team);
        return PacketPlayOutScoreboardTeam.a(t);
    }

    @Override
    public Packet<?> createSetBlockPacket(Block block) {
        return null;
    }

    @Override@SneakyThrows
    public String getItemTypeTranslationKey(Material m) {
        Item item = (Item) craftMagicNumbers().getMethod("getItem", Material.class).invoke(null, m);
        if (item == null) return null;
        return item.getName();
    }

    @Override@SneakyThrows
    public ItemStack asNMSCopy(org.bukkit.inventory.ItemStack itemStack) {
        Class<?> c = Class.forName("org.bukkit.craftbukkit.v1_19_R0.inventory.CraftItemStack");
        return (ItemStack) c.getMethod("asNMSCopy", org.bukkit.inventory.ItemStack.class).invoke(null, itemStack);
    }

    @SneakyThrows
    private Class<?> craftMagicNumbers(){
        return Class.forName("org.bukkit.craftbukkit.v1_19_R0.util.CraftMagicNumbers");
    }

    @Override@SneakyThrows
    public Object getNMSWorld(World world) {
        Class<?> c = Class.forName("org.bukkit.craftbukkit.v1_19_R0.CraftWorld");
        val craftWorld = c.cast(world);
        return craftWorld.getClass().getMethod("getHandle").invoke(craftWorld);
    }

    @Override
    protected void explodeFirework(Player p, Location l, org.bukkit.inventory.ItemStack firework) {
        explodeFirework(p, l, firework, "ai", "ae");
    }
    @Override@SneakyThrows
    public net.minecraft.world.level.block.Block getBlock(Material m) {
        Class<?> c = Class.forName("org.bukkit.craftbukkit.v1_19_R0.util.CraftMagicNumbers");
        return (net.minecraft.world.level.block.Block) c.getMethod("getBlock", Material.class).invoke(null, m);
    }
}
