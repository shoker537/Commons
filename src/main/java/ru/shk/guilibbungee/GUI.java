package ru.shk.guilibbungee;

import dev.simplix.protocolize.api.inventory.InventoryClick;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import dev.simplix.protocolize.data.inventory.InventoryType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import ru.shk.commonsbungee.Commons;
import ru.shk.commonsbungee.ItemStackBuilder;
import ru.shk.guilib.protocolize.ProtocolizeGUI;

import java.util.UUID;
import java.util.function.Consumer;

public class GUI extends ProtocolizeGUI<Plugin, ProxiedPlayer> {

    public GUI(Plugin pl, String name, InventoryType type) {
        super(pl, name, type);
    }

    public GUI(Plugin plugin, String name, InventoryType type, boolean autoColorizeTitle) {
        super(plugin, name, type, autoColorizeTitle);
    }

    public GUI(Plugin plugin, String name, InventoryType type, boolean autoColorizeTitle, boolean jsonTitle) {
        super(plugin, name, type, autoColorizeTitle, jsonTitle);
    }

    @Override
    public void removeGuiFromCache(UUID playerUUID) {
        GUILib.getInstance().getGuis().remove(playerUUID);
    }

    @Override
    public String colorize(String s) {
        return Commons.getInstance().colorize(s);
    }

    @Override
    public void updateBackendInv(ProtocolizePlayer player) {
        Commons.getInstance().async(() -> Commons.getInstance().sendInvUpdate(ProxyServer.getInstance().getPlayer(player.uniqueId())));
    }

    public GUI item(int slot, ItemStackBuilder item, Consumer<InventoryClick> action){
        item(slot, item.build());
        slotActions().put(slot, action);
        update();
        return this;
    }

    @Override
    public void open(ProxiedPlayer p){
        open(p.getUniqueId());
    }

    @Override
    public void open(UUID uuid) {
        super.open(uuid);
        GUILib.getInstance().getGuis().put(uuid, this);
    }

    @Override
    public void close(ProxiedPlayer p){
        close(p.getUniqueId());
    }
}