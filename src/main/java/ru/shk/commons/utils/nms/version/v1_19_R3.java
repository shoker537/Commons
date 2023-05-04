package ru.shk.commons.utils.nms.version;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.SneakyThrows;
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
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import ru.shk.commons.utils.nms.FieldMappings;
import ru.shk.commons.utils.nms.ReflectionUtil;
import ru.shk.commons.utils.nms.Version;

import java.lang.reflect.Method;
import java.util.List;

public class v1_19_R3 extends Version {
    @Override@SneakyThrows
    protected void entityMetadata(Player p, Object e, boolean full) {
        Entity entity = (Entity) e;
        if(!full) sendPacket(p, new ClientboundSetEntityDataPacket(entity.getId(), entity.getEntityData().packDirty()));
//        Class<?> packet = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata");
//        Object dataWatcher = e.getClass().getMethod(FieldMappings.ENTITY_GETDATAWATCHER.getField()).invoke(e);
//        int id = (int) e.getClass().getMethod(FieldMappings.ENTITY_GETID.getField()).invoke(e);
//        Method m = dataWatcher.getEntityData().getClass().getDeclaredMethod("packAll");
//        m.setAccessible(true);
//        Packet<?> pk = (Packet<?>) ConstructorUtils.invokeConstructor(packet, new Object[]{id, m.invoke(dataWatcher)});
//        sendPacket(p, pk);
        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(entity.getId(), (List<SynchedEntityData.DataValue<?>>)ReflectionUtil.runMethod(entity.getEntityData(), "packAll"));
        sendPacket(p, packet);
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
        return ((CraftPlayer)p).getHandle();
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

    @SneakyThrows
    @Override
    protected void equipEntity(Player p, Object e, List<Pair<EquipmentSlot, ItemStack>> items) {
        sendPacket(p, new ClientboundSetEquipmentPacket(entityId(e), items));
    }

    @Override
    protected int getMaterialColorInt(Material m) {
        net.minecraft.world.level.block.Block b = getBlock(m);
        return b.defaultMaterialColor().col;
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
