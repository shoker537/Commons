package ru.shk.guilibbungee;

import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.Protocol;
import dev.simplix.protocolize.api.Protocolize;
import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import ru.shk.commons.utils.Plugin;
import ru.shk.commonsbungee.Commons;
import ru.shk.guilib.protocolize.packet.RenameItemPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class GUILib implements Plugin {
    @Getter private static GUILib instance;
    @Getter private final HashMap<UUID, GUI> guis = new HashMap<>();
    @Getter private static final List<TextInputGUI> textInputGUIS = new ArrayList<>();

    @Override
    public void load() {
        instance = this;
    }

    public void pluginDisabled(net.md_5.bungee.api.plugin.Plugin plugin){
        new HashMap<>(guis).forEach((uuid, gui) -> {
            if(gui.isOpen() && gui.owner()!=null && gui.owner().equals(plugin)){
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
            Commons.getInstance().registerEvents(new GUIEvents());
        } catch (Throwable t){
            Commons.getInstance().info("Unable to register RenameItemPacket.class. Protocolize not found? "+t.getMessage());
        }
    }

    public static class GUIEvents implements Listener {
        @EventHandler
        public void onLeave(PlayerDisconnectEvent e){
            List<TextInputGUI> toRemove = new ArrayList<>();
            for (TextInputGUI gui : textInputGUIS) {
                if(gui.closed(e.getPlayer())) toRemove.add(gui);
            }
            textInputGUIS.removeAll(toRemove);
        }

        @EventHandler
        public void onLeave(ServerSwitchEvent e){
            List<TextInputGUI> toRemove = new ArrayList<>();
            for (TextInputGUI gui : textInputGUIS) {
                if(gui.closed(e.getPlayer())) toRemove.add(gui);
            }
            textInputGUIS.removeAll(toRemove);
        }
    }

    @Override
    public void disable() {

    }
}
