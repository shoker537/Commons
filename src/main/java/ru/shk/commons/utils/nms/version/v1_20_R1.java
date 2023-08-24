package ru.shk.commons.utils.nms.version;

import com.mojang.datafixers.util.Pair;
import lombok.SneakyThrows;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import ru.shk.commons.utils.nms.ReflectionUtil;
import ru.shk.commons.utils.nms.Version;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class v1_20_R1 extends Version {
    @Override@SneakyThrows
    protected void entityMetadata(Player p, Object e, boolean full) {
        Entity entity = (Entity) e;
        if(!full) {
            sendPacket(p, new ClientboundSetEntityDataPacket(entity.getId(), entity.getEntityData().packDirty()));
            return;
        }
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(entity.getId(), (List<SynchedEntityData.DataValue<?>>) ReflectionUtil.runMethod(entity.getEntityData(), "packAll"));
        sendPacket(p, packet);
    }

    @Override
    public void sendPacket(Player p, Packet<?> packet) {
        ((CraftPlayer)p).getHandle().connection.send(packet);
    }

    @Override
    public String getTranslationKey(Material mat) {
        if (mat.isBlock()) {
            net.minecraft.world.level.block.Block b = getBlock(mat);
            return b.getDescriptionId();
        }
        Item item = getItem(mat);
        return item.getDescriptionId();
    }

    @SneakyThrows
    @Override
    public ItemStack asNMSCopy(org.bukkit.inventory.ItemStack itemStack) {
        return CraftItemStack.asNMSCopy(itemStack);
    }

    @Override
    public Object getNMSWorld(World world) {
        return ((CraftWorld)world).getHandle();
    }

    @Override
    protected ServerPlayer getNMSPlayer(Player p) {
        return (ServerPlayer) getNMSEntity(p);
    }

    protected Entity getNMSEntity(org.bukkit.entity.Entity e) {
        return ((CraftEntity)e).getHandle();
    }

    @Override
    public Item getItem(Material m) {
        return CraftMagicNumbers.getItem(m);
    }

    @Override@SneakyThrows
    protected void explodeFirework(Player p, Location l, org.bukkit.inventory.ItemStack firework, String dataWatcherField, String idField) {
        p.playSound(l, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1);
        FireworkRocketEntity fw = new FireworkRocketEntity((ServerLevel)getNMSWorld(l.getWorld()), l.getX(), l.getY(), l.getZ(), asNMSCopy(firework));
        sendPacket(p, new ClientboundAddEntityPacket(fw, 76));
        entityMetadata(p, fw, true);
        sendPacket(p, new ClientboundEntityEventPacket(fw, (byte)17));
        destroyEntity(p, fw.getId());
    }

    @Override
    protected org.bukkit.entity.Entity bukkitEntityFromNMS(Object entity) {
        return ((Entity)entity).getBukkitEntity();
    }

    @Override
    protected void chestOpenState(Location l, boolean open) {
        ServerLevel level = (ServerLevel) getNMSWorld(l.getWorld());
        BlockPos pos = new BlockPos(l.getBlockX(), l.getBlockY(), l.getBlockZ());
        Block b = level.getBlockIfLoaded(pos);
        level.blockEvent(pos, b, 1,open?1:0);
    }

    @SneakyThrows
    @Override
    protected void equipEntity(Player p, Object e, List<Pair<EquipmentSlot, ItemStack>> items) {
        sendPacket(p, new ClientboundSetEquipmentPacket(entityId(e), items));
    }

    @Override
    protected int getMaterialColorInt(Material m) {
        net.minecraft.world.level.block.Block b = getBlock(m);
        return b.defaultMapColor().col;
    }

    @Override
    protected void disableTeammatesCollision(PlayerTeam team) {
        team.setCollisionRule(Team.CollisionRule.PUSH_OTHER_TEAMS);
    }

    @Override
    protected void setCanSeeFriendlyInvisible(PlayerTeam team) {
        team.setSeeFriendlyInvisibles(true);
    }

    @Override
    protected void setFriendlyFire(PlayerTeam team, boolean friendlyFire) {
        team.setAllowFriendlyFire(friendlyFire);
    }

}
