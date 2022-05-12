package ru.shk.guilibbungee;

import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import ru.shk.commons.utils.Plugin;

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

    }

    @Override
    public void disable() {

    }
}
