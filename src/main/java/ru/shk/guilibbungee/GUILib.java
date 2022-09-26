package ru.shk.guilibbungee;

import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.Protocol;
import dev.simplix.protocolize.api.Protocolize;
import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import ru.shk.commons.utils.Plugin;
import ru.shk.commonsbungee.Commons;
import ru.shk.guilibbungee.protocolize.RenameItemPacket;

import java.util.HashMap;
import java.util.UUID;

public class GUILib implements Plugin {
    @Getter private static GUILib instance;
    @Getter private final HashMap<UUID, GUI> guis = new HashMap<>();

    @Override
    public void load() {
        instance = this;
    }

    public void pluginDisabled(net.md_5.bungee.api.plugin.Plugin plugin){
        new HashMap<>(guis).forEach((uuid, gui) -> {
            if(gui.getOwner()!=null && gui.getOwner().equals(plugin)){
                ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuid);
                if(p!=null && p.isConnected()) gui.close(p);
            }
        });
    }

    @Override
    public void enable() {
        try {
            Protocolize.protocolRegistration().registerPacket(RenameItemPacket.MAPPINGS, Protocol.PLAY, PacketDirection.SERVERBOUND, RenameItemPacket.class);
            Commons.getInstance().info("Registered custom packet RenameItemPacket.class");
        } catch (Throwable t){
            Commons.getInstance().info("Unable to register RenameItemPacket.class. Protocolize not found? "+t.getMessage());
        }
    }

    @Override
    public void disable() {

    }
}
