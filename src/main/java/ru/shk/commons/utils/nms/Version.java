package ru.shk.commons.utils.nms;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.SneakyThrows;
import lombok.val;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import ru.shk.commons.Commons;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

public abstract class Version {

    public void sendPacket(Player p, Packet<?> packet) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        Method getHandle = p.getClass().getMethod("getHandle");
        Object nmsPlayer = getHandle.invoke(p);
        Field con_field = nmsPlayer.getClass().getField("b");
        Object con = con_field.get(nmsPlayer);
        Method packet_method = con.getClass().getMethod("a", Packet.class);
        packet_method.invoke(con, packet);
    }

    protected Packet<?> createRemoveTeamPacket(String team) {
        PlayerTeam t = new PlayerTeam(new net.minecraft.world.scores.Scoreboard(), team);
        return ClientboundSetPlayerTeamPacket.createRemovePacket(t);
    }

    protected String getVersionOfPackage(){
        return Commons.getServerVersion().name();
    }

    protected Packet<?> createSetBlockPacket(Block block) {
        return null;
    }

    @SneakyThrows
    public ItemStack asNMSCopy(org.bukkit.inventory.ItemStack itemStack) {
        Class<?> c = Class.forName("org.bukkit.craftbukkit."+getVersionOfPackage()+".inventory.CraftItemStack");
        return (ItemStack) c.getMethod("asNMSCopy", org.bukkit.inventory.ItemStack.class).invoke(null, itemStack);
    }

    protected Class<?> craftMagicNumbers() throws ClassNotFoundException {
        return Class.forName("org.bukkit.craftbukkit."+getVersionOfPackage()+".util.CraftMagicNumbers");
    }

    @SneakyThrows
    public String getItemTypeTranslationKey(Material m) {
        return getTranslationKey(m);
//        Item item = (Item) craftMagicNumbers().getMethod("getItem", Material.class).invoke(null, m);
//        if (item == null) return null;
//        return item.getDescriptionId();
    }

    public String getTranslationKey(Material mat) {
        if (mat.isBlock()) {
            return getBlock(mat).getDescriptionId();
        }
        return getItem(mat).getDescriptionId();
    }

    @SneakyThrows
    public Object getNMSWorld(World world) {
        Class<?> c = Class.forName("org.bukkit.craftbukkit."+getVersionOfPackage()+".CraftWorld");
        val craftWorld = c.cast(world);
        return craftWorld.getClass().getMethod("getHandle").invoke(craftWorld);
    }

    @SneakyThrows
    public net.minecraft.world.level.block.Block getBlock(Material m) {
        Class<?> c = craftMagicNumbers();
        return (net.minecraft.world.level.block.Block) c.getMethod("getBlock", Material.class).invoke(null, m);
    }

    @SneakyThrows
    public net.minecraft.world.item.Item getItem(Material m) {
        Class<?> c = craftMagicNumbers();
        return (net.minecraft.world.item.Item) c.getMethod("getItem", Material.class).invoke(null, m);
    }

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
    protected void equipEntity(Player p, Object e, List<Pair<EquipmentSlot, ItemStack>> items){
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
    protected ServerPlayer getNMSPlayer(Player p){
        return (ServerPlayer) p.getClass().getMethod("getHandle").invoke(p);
    }

    @SneakyThrows
    protected int getMaterialColorInt(Material m){
        net.minecraft.world.level.block.Block b = getBlock(m);
        Object mapColor = b.getClass().getMethod(FieldMappings.BLOCKBASE_GETMAPCOLOR.getField()).invoke(b);
        return mapColor.getClass().getField(FieldMappings.MAPCOLOR_INTCOLOR.getField()).getInt(mapColor);
    }

    @SneakyThrows
    protected void disableTeammatesCollision(PlayerTeam team){
        team.getClass().getMethod(FieldMappings.SCOREBOARDTEAM_SETCOLLISIONMODE.getField(), Team.CollisionRule.class).invoke(team, Team.CollisionRule.PUSH_OTHER_TEAMS);
    }

    @SneakyThrows
    protected void setCanSeeFriendlyInvisible(PlayerTeam team){
        team.getClass().getMethod(FieldMappings.SCOREBOARDTEAM_SETCANSEEFRIENDLYINVISIBLE.getField(), boolean.class).invoke(team, true);
    }

    @SneakyThrows
    protected void setFriendlyFire(PlayerTeam team, boolean friendlyFire){
        team.getClass().getMethod(FieldMappings.SCOREBOARDTEAM_SETFRIENDLYFIRE.getField(), boolean.class).invoke(team, friendlyFire);
    }

    protected Packet<?> createScoreboardTeamPacket(boolean createTeamOrUpdate, boolean collideTeammates, boolean friendlyFire, boolean seeFriendlyInvisible, String name, String prefix, String suffix, ChatColor color, List<String> entries) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        PlayerTeam t = new PlayerTeam(new net.minecraft.world.scores.Scoreboard(), name);
        if(!collideTeammates) disableTeammatesCollision(t);
        if(seeFriendlyInvisible) setCanSeeFriendlyInvisible(t);
        setFriendlyFire(t, friendlyFire);
        t.getClass().getMethod("b", Component.class).invoke(t, Component.nullToEmpty(prefix));
        t.getClass().getMethod("c", Component.class).invoke(t, Component.nullToEmpty(suffix));
        if(color!=null) t.getClass().getMethod("a", ChatFormatting.class).invoke(t, ChatFormatting.valueOf(color.name()));
        if(entries!=null){
            Collection<String> e = (Collection<String>) t.getClass().getMethod("g").invoke(t);
            e.addAll(entries);
        }
        return ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(t, createTeamOrUpdate);
    }

    protected Packet<?> createScoreboardTeamPacket(boolean createTeamOrUpdate, boolean collideTeammates, String name, String prefix, String suffix, ChatColor color, List<String> entries) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return createScoreboardTeamPacket(createTeamOrUpdate, collideTeammates, true, false, name, prefix, suffix, color, entries);
    }

    protected Packet<?> createScoreboardTeamPacket(boolean createTeamOrUpdate, String name, String prefix, String suffix) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return createScoreboardTeamPacket(createTeamOrUpdate, true,name, prefix, suffix, null, null);
    }

    protected void explodeFirework(Player p, Location l, org.bukkit.inventory.ItemStack firework) {
        explodeFirework(p, l, firework, "ai", "ae");
    }

    @SneakyThrows
    protected void playRiptideAnimation(Player p, int ticks){
        Class<?> c = Class.forName("org.bukkit.craftbukkit."+getVersionOfPackage()+".entity.CraftPlayer");
        val craftPlayer = c.cast(p);
        net.minecraft.world.entity.player.Player pl = (net.minecraft.world.entity.player.Player) craftPlayer.getClass().getMethod("getHandle").invoke(craftPlayer);
        pl.startAutoSpinAttack(ticks);
    }

}
