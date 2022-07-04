package ru.shk.guilibbungee;

import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.inventory.Inventory;
import dev.simplix.protocolize.api.inventory.InventoryClick;
import dev.simplix.protocolize.api.inventory.InventoryClose;
import dev.simplix.protocolize.api.item.ItemStack;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import dev.simplix.protocolize.data.ItemType;
import dev.simplix.protocolize.data.inventory.InventoryType;
import dev.simplix.protocolize.data.packets.SetSlot;
import lombok.Getter;
import lombok.val;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import ru.shk.commonsbungee.Commons;
import ru.shk.commonsbungee.ItemStackBuilder;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

public class GUI extends Inventory {
    private final HashMap<Integer, Consumer<InventoryClick>> slotActions = new HashMap<>();
    private Consumer<InventoryClick> universalAction = inventoryClick -> {};
    @Getter private Plugin owner;
    private UUID pp;

    public GUI(Plugin pl, String name, InventoryType type){
        this(name, type);
        this.owner = pl;
    }

    public void clear(int itemSlot){
        item(itemSlot, new ItemStack(ItemType.AIR));
        slotActions.remove(itemSlot);
        update();
    }

    public void clear(){
        slotActions.clear();
        universalAction = inventoryClick -> {};
        new HashMap<>(items()).forEach((integer, itemStack) -> item(integer, new ItemStack(ItemType.AIR)));
    }

    @Deprecated
    public GUI(String name, InventoryType type) {
        super(type);
        title(Commons.getInstance().colorize(name));
        super.onClick(inventoryClick-> {
            inventoryClick.cancelled(true);
            slotActions.getOrDefault(inventoryClick.slot(), universalAction).accept(inventoryClick);
        });
    }

    @Override
    public GUI onClick(Consumer<InventoryClick> consumer) {
        universalAction = consumer;
        return this;
    }

    public GUI item(int slot, ItemStack item, Consumer<InventoryClick> action){
        item(slot, item);
        slotActions.put(slot, action);
        update();
        return this;
    }

    public GUI item(int slot, ItemStackBuilder item, Consumer<InventoryClick> action){
        item(slot, item.build());
        slotActions.put(slot, action);
        update();
        return this;
    }

    public void open(ProxiedPlayer p){
        pp = p.getUniqueId();
        GUILib.getInstance().getGuis().put(p.getUniqueId(), this);
        ProtocolizePlayer player = Protocolize.playerProvider().player(p.getUniqueId());
        player.closeInventory();
        player.openInventory(this);
    }

    @Override
    public Inventory onClose(Consumer<InventoryClose> consumer) {
        val b = super.onClose(consumer);
        GUILib.getInstance().getGuis().remove(pp);
        return b;
    }

    public void close(ProxiedPlayer p){
        ProtocolizePlayer player = Protocolize.playerProvider().player(p.getUniqueId());
        player.closeInventory();
    }

    public void update(){
        if(pp==null) {
            ProxyServer.getInstance().getLogger().warning("Tried to update an inventory when the viewer is null!");
            return;
        }
        int windowId = -1;
        ProtocolizePlayer player = Protocolize.playerProvider().player(pp);
        for (Integer id : player.registeredInventories().keySet()) {
            if(player.registeredInventories().get(id).equals(this)) {
                windowId = id;
                break;
            }
        }
        if(windowId==-1) {
            ProxyServer.getInstance().getPlayer(pp).sendMessage(ChatColor.RED+"WindowId not found. GUI not registered yet?");
            return;
        }
        int finalWindowId = windowId;
        try {
            this.items().forEach((integer, itemStack) -> player.sendPacket(new SetSlot((byte) finalWindowId, (short) ((int)integer), itemStack, 0)));
        } catch (Throwable t){
            ProxyServer.getInstance().getLogger().warning(t.getMessage());
        }
    }
}