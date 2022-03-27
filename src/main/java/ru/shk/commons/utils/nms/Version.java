package ru.shk.commons.utils.nms;


import net.minecraft.network.protocol.Packet;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

public interface Version {
    void sendPacket(Player p, Packet<?> packet) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException;
    Packet<?> createScoreboardTeamPacket(String name, String prefix, String suffix) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException;
    String getItemTypeTranslationKey(Material m);
}
