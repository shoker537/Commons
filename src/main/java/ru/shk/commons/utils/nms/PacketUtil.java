package ru.shk.commons.utils.nms;

import net.minecraft.network.protocol.Packet;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.shk.commons.Commons;

import java.lang.reflect.InvocationTargetException;

public class PacketUtil {

    private static final Version versionClass;
    static {
        Version versionClass1;
        try {
            Class<? extends Version> cl = (Class<? extends Version>) Class.forName("ru.shk.commons.utils.nms.version."+Commons.getServerVersion());
            versionClass1 = cl.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            versionClass1 = null;
        }
        versionClass = versionClass1;
    }

    public static void sendPacket(Player p, Packet<?>... packets) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        for (Packet<?> packet : packets) versionClass.sendPacket(p, packet);
    }

    public static void sendScoreboardTeamPacket(Player p, String name, String prefix, String suffix) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
        Packet<?> packet = versionClass.createScoreboardTeamPacket(name, prefix, suffix);
        sendPacket(p, packet);
    }

    public static String getItemTypeTranslationKey(Material material) {
        if (material == null) return null;
        return versionClass.getItemTypeTranslationKey(material);
    }
}
