package ru.shk.commons.utils.nms;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.SneakyThrows;
import lombok.val;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.map.MapPalette;
import ru.shk.commons.Commons;

import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CurrentVersion {

    public void sendPacket(Player p, Packet<?> packet) {
        ((CraftPlayer)p).getHandle().connection.send(packet);
    }

    public void sendMap(List<Player> players, int mapId, BufferedImage image){
        val packet = createMapPacket(mapId, image);
        players.forEach(player -> sendPacket(player, packet));
    }

    public Packet<?> createMapPacket(int mapId, BufferedImage image){
        val mapPatch = new MapItemSavedData.MapPatch(0, 0, 128, 128, imageToByteArray(image));
        return new ClientboundMapItemDataPacket(mapId, (byte) 0, false, Collections.emptyList(), mapPatch);
    }

    private static byte[] imageToByteArray(BufferedImage image){
        return MapPalette.imageToBytes(image);
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


    public ItemStack asNMSCopy(org.bukkit.inventory.ItemStack itemStack) {
        return CraftItemStack.asNMSCopy(itemStack);
    }

    protected Class<?> craftMagicNumbers() throws ClassNotFoundException {
        return Class.forName("org.bukkit.craftbukkit."+getVersionOfPackage()+".util.CraftMagicNumbers");
    }
    protected Class<?> craftItemStack() throws ClassNotFoundException {
        return Class.forName("org.bukkit.craftbukkit."+getVersionOfPackage()+".inventory.CraftItemStack");
    }

    @SneakyThrows
    public String getItemTypeTranslationKey(Material m) {
        return getTranslationKey(m);
    }

    public String getTranslationKey(Material mat) {
        if (mat.isBlock()) {
            net.minecraft.world.level.block.Block b = getBlock(mat);
            return b.getDescriptionId();
        }
        Item item = getItem(mat);
        return item.getDescriptionId();
    }


    public Object getNMSWorld(World world) {
        return ((CraftWorld)world).getHandle();
    }

    @SneakyThrows
    public net.minecraft.world.level.block.Block getBlock(Material m) {
        Class<?> c = craftMagicNumbers();
        return (net.minecraft.world.level.block.Block) c.getMethod("getBlock", Material.class).invoke(null, m);
    }

    public Item getItem(Material m) {
        return CraftMagicNumbers.getItem(m);
    }

    @SneakyThrows
    protected void explodeFirework(Player p, Location l, org.bukkit.inventory.ItemStack firework) {
        p.playSound(l, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1);
        FireworkRocketEntity fw = new FireworkRocketEntity((ServerLevel)getNMSWorld(l.getWorld()), l.getX(), l.getY(), l.getZ(), asNMSCopy(firework));
        sendPacket(p, new ClientboundAddEntityPacket(fw, 76));
        entityMetadata(p, fw, true);
        sendPacket(p, new ClientboundEntityEventPacket(fw, (byte)17));
        destroyEntity(p, fw.getId());
    }

    @SneakyThrows
    protected void spawnLivingEntity(Player p, Object e){
        spawnEntity(p, e);
    }
    @SneakyThrows
    protected void spawnEntity(Player p, Object e){
        sendPacket(p, new ClientboundAddEntityPacket((Entity) e));
    }

    @SneakyThrows
    protected void entityAnimation(Player p, Entity entity, int animation){
        if(animation==1) {
            sendPacket(p, new ClientboundDamageEventPacket(entity, entity.level().damageSources().generic()));
        } else {
            sendPacket(p, new ClientboundAnimatePacket(entity, animation));
        }
    }

    @SneakyThrows
    protected void spawnPlayer(Player p, Object e){
        sendPacket(p, new ClientboundAddPlayerPacket((net.minecraft.world.entity.player.Player) e));
    }
    @SneakyThrows
    protected void destroyEntity(Player p, Object e){
        destroyEntity(p, entityId(e));
    }
    @SneakyThrows
    protected void destroyEntity(Player p, int id){
        sendPacket(p, new ClientboundRemoveEntitiesPacket(IntList.of(id)));
    }

    protected void equipEntity(Player p, Object e, List<Pair<EquipmentSlot, ItemStack>> items) {
        sendPacket(p, new ClientboundSetEquipmentPacket(entityId(e), items));
    }

    @SneakyThrows
    public void addPlayerProfile(Player p, ServerPlayer player){
        sendPacket(p, new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, player));
    }
    @SneakyThrows
    public void removePlayerProfiles(Player p, List<UUID> uuids){
        sendPacket(p, new ClientboundPlayerInfoRemovePacket(uuids));
    }
    @SneakyThrows
    protected int entityId(Object e){
        return ((Entity)e).getId();
    }

    @SneakyThrows
    protected void entityMetadata(Player p, Object e){
        entityMetadata(p, e, true);
    }
    @SneakyThrows
    protected void entityMetadata(Player p, Object e, boolean full) {
        Entity entity = (Entity) e;
        if(!full) {
            sendPacket(p, new ClientboundSetEntityDataPacket(entity.getId(), entity.getEntityData().packDirty()));
            return;
        }
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(entity.getId(), (List<SynchedEntityData.DataValue<?>>) ReflectionUtil.runMethod(entity.getEntityData(), "packAll"));
        sendPacket(p, packet);
    }

    @SneakyThrows
    protected void teleportEntity(Player p, Entity e){
        sendPacket(p, new ClientboundTeleportEntityPacket(e));
    }
    @SneakyThrows
    protected void leashPacket(Player p, org.bukkit.entity.Entity owner, org.bukkit.entity.Entity attached){
        ClientboundSetEntityLinkPacket packet = new ClientboundSetEntityLinkPacket(getNMSEntity(attached), getNMSEntity(owner));
        sendPacket(p, packet);
    }

    protected org.bukkit.entity.Entity bukkitEntityFromNMS(Object entity) {
        return ((Entity)entity).getBukkitEntity();
    }


    protected void chestOpenState(Location l, boolean open) {
        ServerLevel level = (ServerLevel) getNMSWorld(l.getWorld());
        BlockPos pos = new BlockPos(l.getBlockX(), l.getBlockY(), l.getBlockZ());
        net.minecraft.world.level.block.Block b = level.getBlockIfLoaded(pos);
        level.blockEvent(pos, b, 1,open?1:0);
    }
    @SneakyThrows
    protected void playTotemAnimation(Player p){
//        Class<?> statusPacket = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityStatus");
//        val a = (Packet<?>) ConstructorUtils.invokeConstructor(statusPacket, new Object[]{getNMSPlayer(p), (byte)35});
        sendPacket(p, new ClientboundEntityEventPacket(getNMSPlayer(p), (byte)35));
    }
    protected ServerPlayer getNMSPlayer(Player p) {
        return (ServerPlayer) getNMSEntity(p);
    }
    protected Entity getNMSEntity(org.bukkit.entity.Entity e) {
        return ((CraftEntity)e).getHandle();
    }


    protected int getMaterialColorInt(Material m) {
        net.minecraft.world.level.block.Block b = getBlock(m);
        return b.defaultMapColor().col;
    }


    protected void disableTeammatesCollision(PlayerTeam team) {
        team.setCollisionRule(Team.CollisionRule.PUSH_OTHER_TEAMS);
    }


    protected void setCanSeeFriendlyInvisible(PlayerTeam team) {
        team.setSeeFriendlyInvisibles(true);
    }


    protected void setFriendlyFire(PlayerTeam team, boolean friendlyFire) {
        team.setAllowFriendlyFire(friendlyFire);
    }

    protected Packet<?> createScoreboardTeamPacket(boolean createTeam, boolean collideTeammates, boolean friendlyFire, boolean seeFriendlyInvisible, String name, String prefix, String suffix, ChatColor color, List<String> entries) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return createScoreboardTeamPacket(createTeam, collideTeammates, friendlyFire, seeFriendlyInvisible, name, Component.nullToEmpty(prefix), Component.nullToEmpty(suffix), color, entries);
    }

    protected Packet<?> createScoreboardTeamPacket(boolean createTeam, boolean collideTeammates, boolean friendlyFire, boolean seeFriendlyInvisible, String name, Component prefix, Component suffix, ChatColor color, List<String> entries) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        PlayerTeam t = new PlayerTeam(new net.minecraft.world.scores.Scoreboard(), name);
        if(!collideTeammates) disableTeammatesCollision(t);
        if(seeFriendlyInvisible) setCanSeeFriendlyInvisible(t);
        setFriendlyFire(t, friendlyFire);
        t.setPlayerPrefix(prefix);
        t.setPlayerSuffix(suffix);
        if(color!=null) t.setColor(ChatFormatting.valueOf(color.name()));
        if(entries!=null) t.getPlayers().addAll(entries);
        return ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(t, createTeam);
    }

    protected Packet<?> createScoreboardTeamPacket(boolean createTeam, boolean collideTeammates, String name, Component prefix, Component suffix, ChatColor color, List<String> entries) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return createScoreboardTeamPacket(createTeam, collideTeammates, true, false, name, prefix, suffix, color, entries);
    }
    protected Packet<?> createScoreboardTeamPacket(boolean createTeam, boolean collideTeammates, String name, String prefix, String suffix, ChatColor color, List<String> entries) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return createScoreboardTeamPacket(createTeam, collideTeammates, true, false, name, prefix, suffix, color, entries);
    }
    protected Packet<?> createScoreboardTeamPacket(boolean createTeam, String name, String prefix, String suffix) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return createScoreboardTeamPacket(createTeam, true,name, prefix, suffix, null, null);
    }
    protected Packet<?> createScoreboardTeamPacket(boolean createTeam, String name, net.kyori.adventure.text.Component prefix, net.kyori.adventure.text.Component suffix) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return createScoreboardTeamPacket(createTeam, true, name, net.minecraft.network.chat.Component.Serializer.fromJson(GsonComponentSerializer.gson().serialize(prefix)), net.minecraft.network.chat.Component.Serializer.fromJson(GsonComponentSerializer.gson().serialize(suffix)), null, null);
    }

    @SneakyThrows
    protected void playRiptideAnimation(Player p, int ticks){
        getNMSPlayer(p).startAutoSpinAttack(ticks);
    }

}
