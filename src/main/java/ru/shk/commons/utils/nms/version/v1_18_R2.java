package ru.shk.commons.utils.nms.version;

import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam;
import net.minecraft.world.item.Item;
import net.minecraft.world.scores.ScoreboardTeam;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import ru.shk.commons.utils.nms.Version;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class v1_18_R2 implements Version {
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
    public Packet<?> createScoreboardTeamPacket(String name, String prefix, String suffix) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ScoreboardTeam t = new ScoreboardTeam(new net.minecraft.world.scores.Scoreboard(), name);
        t.getClass().getMethod("b", IChatBaseComponent.class).invoke(t, IChatBaseComponent.a(prefix));
        t.getClass().getMethod("c", IChatBaseComponent.class).invoke(t, IChatBaseComponent.a(suffix));
        return PacketPlayOutScoreboardTeam.a(t, false);
    }

    @Override
    public String getItemTypeTranslationKey(Material m) {
        Item nmsItem = CraftMagicNumbers.getItem(m);
        if (nmsItem == null) return null;
        return nmsItem.getName();
    }

}
