package ru.shk.commons.utils.nms;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.SneakyThrows;
import lombok.val;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.map.MapPalette;
import ru.shk.commons.Commons;

import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
        return new ClientboundMapItemDataPacket(new MapId(mapId), (byte) 0, false, Collections.emptyList(), mapPatch);
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
        return CraftMagicNumbers.getBlock(m);
    }

    public Item getItem(Material m) {
        return CraftMagicNumbers.getItem(m);
    }

    @SneakyThrows
    protected void explodeFirework(Player p, Location l, org.bukkit.inventory.ItemStack firework) {
        p.playSound(l, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1);
        FireworkRocketEntity fw = new FireworkRocketEntity((ServerLevel)getNMSWorld(l.getWorld()), l.getX(), l.getY(), l.getZ(), asNMSCopy(firework));
        sendPacket(p, new ClientboundAddEntityPacket(fw, 76, new BlockPos(l.getBlockX(), l.getBlockY(), l.getBlockZ())));
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
        Entity entity = (Entity) e;
        sendPacket(p, new ClientboundAddEntityPacket(entity,0, new BlockPos(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ())));
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
        net.minecraft.world.entity.player.Player player =(net.minecraft.world.entity.player.Player) e;
        sendPacket(p, new ClientboundAddEntityPacket(player, 0, new BlockPos(player.getBlockX(), player.getBlockY(), player.getBlockZ())));
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
    protected void updateInventory(Player p, int containerId, int stateId, List<ItemStack> items, ItemStack cursor) {
        NonNullList list = NonNullList.createWithCapacity(items.size());
        list.addAll(items);
        sendPacket(p, new ClientboundContainerSetContentPacket(containerId, stateId, list, cursor));
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
    protected void teleportEntity(Player p, Entity e, double x, double y, double z, float yaw, float pitch){
        PositionMoveRotation pos = new PositionMoveRotation(new Vec3(x,y,z), new Vec3(0,0,0), yaw, pitch);
        sendPacket(p, ClientboundTeleportEntityPacket.teleport(e.getId(), pos, Set.of(), e.onGround));
    }

    @SneakyThrows
    protected void teleportEntity(Player p, Entity e){
        sendPacket(p, ClientboundTeleportEntityPacket.teleport(e.getId(), PositionMoveRotation.of(e), Set.of(), e.onGround));
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

    protected Packet<?> createScoreboardTeamPacket(boolean createTeam, boolean collideTeammates, String name, net.kyori.adventure.text.Component prefix, net.kyori.adventure.text.Component suffix, ChatColor color, List<String> entries) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return createScoreboardTeamPacket(createTeam, collideTeammates, true, false, name, toMojangComponent(prefix), toMojangComponent(suffix), color, entries);
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
        return createScoreboardTeamPacket(createTeam, true, name, net.minecraft.network.chat.Component.Serializer.fromJson(GsonComponentSerializer.gson().serialize(prefix), RegistryAccess.EMPTY), net.minecraft.network.chat.Component.Serializer.fromJson(GsonComponentSerializer.gson().serialize(suffix), RegistryAccess.EMPTY), null, null);
    }

    protected Component toMojangComponent(net.kyori.adventure.text.Component c){
        return Component.Serializer.fromJson(GsonComponentSerializer.gson().serialize(c), RegistryAccess.EMPTY);
    }

    @SneakyThrows
    protected void playRiptideAnimation(Player p, int ticks){
        getNMSPlayer(p).startAutoSpinAttack(ticks, 0, null);
    }

}
