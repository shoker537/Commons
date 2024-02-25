package ru.shk.commons.utils.nms.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import lombok.Getter;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.player.PlayerKickEvent;
import org.jetbrains.annotations.Nullable;
import ru.shk.commons.utils.nms.PacketUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class PacketPlayer extends PacketEntity<PacketPlayer> {
    @Getter private final GameProfile gameProfile;

    public PacketPlayer(Location l, GameProfile gameProfile) throws RuntimeException {
        super("net.minecraft.world.entity.player.Player", "player", l.getWorld());
        gameProfile.getProperties().put("textures", new Property("texture", "ewogICJ0aW1lc3RhbXAiIDogMTcwMTQwMjQ4NTY3NSwKICAicHJvZmlsZUlkIiA6ICIxMjcxYWE1MzA5NDk0MWFhYjM3ZGY2YjZiZTEwZjgzYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJTSE9LRVIxMzciLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODA2MDllYjljODUwYTY5ZjVlMWMwZThlODRiNmQxZDQ4ZjFkZTYyMmQyYTMxMjNjMWNkODkzYzY3MzE4MjQ4NyIKICAgIH0sCiAgICAiQ0FQRSIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjlhNzY1Mzc2NDc5ODlmOWEwYjZkMDAxZTMyMGRhYzU5MWMzNTllOWU2MWEzMWY0Y2UxMWM4OGYyMDdmMGFkNCIKICAgIH0KICB9Cn0="));
        this.gameProfile = gameProfile;
        try {
            entity = new ServerPlayer(MinecraftServer.getServer(), (ServerLevel) PacketUtil.getNMSWorld(l.getWorld()), gameProfile);
            ((ServerPlayer)entity).connection = new FakeConnection((ServerPlayer) entity);
        } catch (Throwable t){
            throw new RuntimeException(t);
        }
        teleport(l);
    }

    @SneakyThrows
    @Override
    public synchronized void teleport(World w, double x, double y, double z, float yaw, float pitch, boolean sendPackets) {
        super.teleport(w, x, y, z, yaw, pitch, sendPackets);
        int i = Mth.floor(entity.getYRot() * 256.0F / 360.0F);
        receivers.forEach(player -> {
            try {
                PacketUtil.sendPacket(player, new ClientboundRotateHeadPacket(entity, (byte) i));
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void createEntity(World world) {}


    @Override@SneakyThrows
    public synchronized void spawn(Player player) {
        sendAddProfilePacket(player);
        byte cape = 0x01;
        byte jacket = 0x02;
        byte sleeve1 = 0x04;
        byte sleeve2 = 0x08;
        byte leg1 = 0x10;
        byte leg2 = 0x20;
        byte hat = 0x40;
        byte allskin = (byte) (cape | jacket | sleeve1 | sleeve2 | leg1 | leg2 | hat);
        entity.getEntityData().set(net.minecraft.world.entity.player.Player.DATA_PLAYER_MODE_CUSTOMISATION, allskin, true);
        super.spawn(player);
    }

    public void playAnimation(Animation animation){
        receivers.forEach(player -> playAnimation(player, animation));
    }

    public void playAnimation(Player player, Animation animation){
        PacketUtil.sendEntityAnimation(player, entity, animation.id);
    }

    @Override
    protected void sendSpawnPacket(Player p) {
        PacketUtil.spawnPlayer(p, entity);
    }

    @Override
    public synchronized void despawn(Player player) {
        super.despawn(player);
        sendRemoveProfilePacket(player);
    }

    private void sendAddProfilePacket(Player player){
        PacketUtil.sendAddPlayerProfile(player, entity);
    }
    private void sendRemoveProfilePacket(Player player){
        PacketUtil.sendRemovePlayerProfiles(player, List.of(entity.getUUID()));
    }

    public void setSneaking(boolean sneaking){
        entity.setSharedFlag(1, sneaking);
        if(sneaking) pose(org.bukkit.entity.Pose.SNEAKING, false); else pose(Pose.STANDING, false);
        metadata();
    }

    public static class FakeConnection extends ServerGamePacketListenerImpl {

        public FakeConnection(ServerPlayer player) {
            super(MinecraftServer.getServer(), new Connection(PacketFlow.CLIENTBOUND), player);
        }

        @Override
        public void resetPosition() {}

        @Override
        public void tick() {}

        @Override
        public boolean isAcceptingMessages() {
            return false;
        }

        @Override
        public void disconnect(Component reason, PlayerKickEvent.Cause cause) {}

        @Override
        public void send(Packet<?> packet, @Nullable PacketSendListener callbacks) {}

        @Override
        public void handleAcceptTeleportPacket(ServerboundAcceptTeleportationPacket packet) {}
    }

    public enum Animation {
        SHAKE_MAIN_HAND(0),
        SHAKE_OFF_HAND(3),
        TAKE_DAMAGE(1)

        ;

        private final int id;

        Animation(int id) {
            this.id = id;
        }
    }
}
