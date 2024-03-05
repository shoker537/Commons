package ru.shk.velocity.commons.gui;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import dev.simplix.protocolize.data.inventory.InventoryType;
import ru.shk.guilib.protocolize.ProtocolizeGUI;
import ru.shk.velocity.commons.Commons;
import ru.shk.velocity.commons.utils.PluginMessage;

import java.util.Optional;
import java.util.UUID;

public class GUI extends ProtocolizeGUI<Object, Player> {
    public GUI(Object pl, String name, InventoryType type) {
        super(pl, name, type);
    }

    public GUI(Object o, String name, InventoryType type, boolean autoColorizeTitle) {
        super(o, name, type, autoColorizeTitle);
    }

    public GUI(Object o, String name, InventoryType type, boolean autoColorizeTitle, boolean jsonTitle) {
        super(o, name, type, autoColorizeTitle, jsonTitle);
    }

    @Override
    public void removeGuiFromCache(UUID playerUUID) {
        //TODO
    }

    @Override
    public String colorize(String s) {
        return s.replace('&','§');
    }

    @Override
    public void updateBackendInv(ProtocolizePlayer player) {
        Optional<Player> p = Commons.getInstance().proxy().getPlayer(player.uniqueId());
        if(p.isEmpty()) return;
        Optional<ServerConnection> s = p.get().getCurrentServer();
        if(s.isEmpty()) return;
        new PluginMessage("commons:updateinv").writeUTF(player.uniqueId().toString()).send(s.get());
    }

    @Override
    public void open(Player p) {
        open(p.getUniqueId());
    }

    @Override
    public void close(Player p) {
        close(p.getUniqueId());
    }

}
