package ru.shk.guilibbungee;

import dev.simplix.protocolize.api.ClickType;
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
    @Getter private boolean open = false;
    private int state = 0;

    public GUI(Plugin pl, String name, InventoryType type){
        this(name, type);
        this.owner = pl;
    }

    public void clear(int itemSlot){
        removeItem(itemSlot);
        item(itemSlot, new ItemStack(ItemType.AIR));
        slotActions.remove(itemSlot);
        update();
    }

    public void clear(){
        slotActions.clear();
        universalAction = inventoryClick -> {};
        new HashMap<>(items()).forEach((integer, itemStack) -> removeItem(integer));
    }

    @Deprecated
    public GUI(String name, InventoryType type) {
        super(type);
        title(Commons.getInstance().colorize(name));
        super.onClick(inventoryClick-> {
            inventoryClick.cancelled(true);
            if(inventoryClick.clickType()==ClickType.SHIFT_LEFT_CLICK || inventoryClick.clickType()==ClickType.SHIFT_RIGHT_CLICK) {
                Commons.getInstance().async(() -> {
                    ProxiedPlayer p = ProxyServer.getInstance().getPlayer(pp);
                    Commons.getInstance().sendInvUpdate(p);
                });
            }
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
        open = true;
    }

    @Override
    public Inventory onClose(Consumer<InventoryClose> consumer) {
        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(pp);
        val b = super.onClose(consumer);
        GUILib.getInstance().getGuis().remove(pp);
        return b;
    }

    public void close(ProxiedPlayer p){
        open = false;
        ProtocolizePlayer player = Protocolize.playerProvider().player(p.getUniqueId());
        player.closeInventory();
    }

    private int getInvId(){
        int windowId = -1;
        ProtocolizePlayer player = Protocolize.playerProvider().player(pp);
        for (Integer id : player.registeredInventories().keySet()) {
            if(player.registeredInventories().get(id).equals(this)) {
                windowId = id;
                break;
            }
        }
        return windowId;
    }

    public void update(){
        state++;
        try {
            if(!open) {
                return;
            }
            if(pp==null) {
                return;
            }
            ProtocolizePlayer player = Protocolize.playerProvider().player(pp);
            int windowId = getInvId();
            ProxiedPlayer p = ProxyServer.getInstance().getPlayer(pp);
            if(windowId==-1) {
                p.sendMessage(ChatColor.RED+"WindowId not found. GUI not registered yet?");
                return;
            }
            this.items().forEach((integer, itemStack) -> player.sendPacket(new SetSlot((byte) windowId, (short) ((int)integer), itemStack, state)));
        } catch (Throwable t){
            t.printStackTrace();
        }
    }
}