package ru.shk.guilibbungee;

import com.google.common.collect.Lists;
import dev.simplix.protocolize.api.ClickType;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.inventory.Inventory;
import dev.simplix.protocolize.api.inventory.InventoryClick;
import dev.simplix.protocolize.api.inventory.InventoryClose;
import dev.simplix.protocolize.api.item.BaseItemStack;
import dev.simplix.protocolize.api.item.ItemStack;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import dev.simplix.protocolize.data.ItemType;
import dev.simplix.protocolize.data.inventory.InventoryType;
import dev.simplix.protocolize.data.packets.SetSlot;
import dev.simplix.protocolize.data.packets.WindowItems;
import lombok.Getter;
import lombok.val;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import ru.shk.commonsbungee.Commons;
import ru.shk.commonsbungee.ItemStackBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class GUI extends Inventory {
    private final HashMap<Integer, Consumer<InventoryClick>> slotActions = new HashMap<>();
    private Consumer<InventoryClick> universalAction = inventoryClick -> {};
    private Consumer<InventoryClose> closeConsumer = null;
    @Getter private final Plugin owner;
    private UUID pp;
    @Getter private boolean open = false;
    private int state = 0;
    private int windowId = -1;

    public GUI(Plugin pl, String name, InventoryType type){
        this(pl, name, type, true);

        super.onClose(close -> {
            try {
                GUILib.getInstance().getGuis().remove(pp);
            } catch (Throwable t){
                t.printStackTrace();
            }
            if(closeConsumer!=null) closeConsumer.accept(close);
        });
    }

    public GUI(Plugin pl, String name, InventoryType type, boolean autoColorizeTitle){
        this(pl, name, type, autoColorizeTitle, false);
    }
    public GUI(Plugin pl, String name, InventoryType type, boolean autoColorizeTitle, boolean jsonTitle){
        super(type);
        this.owner = pl;
        String colorizedTitle = autoColorizeTitle?Commons.getInstance().colorize(name):name;
        if(jsonTitle) titleJson(colorizedTitle); else title(colorizedTitle);
        super.onClick(inventoryClick-> {
            inventoryClick.cancelled(true);
            Commons.getInstance().async(() -> {
                ProxiedPlayer p = ProxyServer.getInstance().getPlayer(pp);
                Commons.getInstance().sendInvUpdate(p);
            });
            slotActions.getOrDefault(inventoryClick.slot(), universalAction).accept(inventoryClick);
        });
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
        windowId = getInvId();
        open = true;
    }

    @Override
    public Inventory onClose(Consumer<InventoryClose> consumer) {
        closeConsumer = consumer;
        return this;
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

    public void reopen(ProxiedPlayer player){
        if(!isOpen()) return;
        close(player);
        open(player);
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
            ProxiedPlayer p = ProxyServer.getInstance().getPlayer(pp);
            if(windowId==-1) {
                p.sendMessage(ChatColor.RED+"WindowId not found. GUI not registered yet?");
                return;
            }
            List<BaseItemStack> items = new ArrayList<>(Lists.newArrayList(itemsIndexed(player.protocolVersion())));
            player.sendPacket(new WindowItems((short) windowId, items, state));
//            this.items().forEach((integer, itemStack) -> player.sendPacket(new SetSlot((byte) windowId, (short) ((int)integer), itemStack, state)));
        } catch (Throwable t){
            t.printStackTrace();
        }
    }
}