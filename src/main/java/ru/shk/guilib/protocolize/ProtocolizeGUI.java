package ru.shk.guilib.protocolize;

import com.google.common.collect.Lists;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.inventory.Inventory;
import dev.simplix.protocolize.api.inventory.InventoryClick;
import dev.simplix.protocolize.api.inventory.InventoryClose;
import dev.simplix.protocolize.api.item.BaseItemStack;
import dev.simplix.protocolize.api.item.ItemStack;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import dev.simplix.protocolize.data.ItemType;
import dev.simplix.protocolize.data.inventory.InventoryType;
import dev.simplix.protocolize.data.packets.WindowItems;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import ru.shk.commons.utils.Logger;
import ru.shk.commons.utils.items.ItemStackBuilder;
import ru.shk.commons.utils.items.bungee.BungeeItemStack;
import ru.shk.guilibbungee.GUILib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Accessors(fluent = true)
public abstract class ProtocolizeGUI<PLUGIN, PLAYER> extends Inventory {
    private static final Consumer<InventoryClick> EMPTY_CLICK = click -> {};

    @Getter private final HashMap<Integer, Consumer<InventoryClick>> slotActions = new HashMap<>();
    @Getter private Consumer<InventoryClick> universalAction = EMPTY_CLICK;
    @Getter private Consumer<InventoryClose> closeConsumer = null;
    @Getter
    private final PLUGIN owner;
    @Setter@Getter private UUID pp;
    @Accessors(fluent = false)@Getter@Setter private boolean open = false;
    private int state = 0;
    @Setter@Getter private int windowId = -1;

    public ProtocolizeGUI(PLUGIN pl, String name, InventoryType type){
        this(pl, name, type, true);
        super.onClose(close -> {
            try {
                removeGuiFromCache(close.player().uniqueId());
            } catch (Throwable t){
                t.printStackTrace();
            }
            if(closeConsumer!=null) closeConsumer.accept(close);
        });
    }

    public abstract void removeGuiFromCache(UUID playerUUID);

    public ProtocolizeGUI(PLUGIN plugin, String name, InventoryType type, boolean autoColorizeTitle){
        this(plugin, name, type, autoColorizeTitle, false);
    }

    public ProtocolizeGUI(PLUGIN plugin, String name, InventoryType type, boolean autoColorizeTitle, boolean jsonTitle){
        super(type);
        this.owner = plugin;
        String colorizedTitle = autoColorizeTitle?colorize(name):name;
        if(jsonTitle) titleJson(colorizedTitle); else title(colorizedTitle);
        super.onClick(inventoryClick-> {
            inventoryClick.cancelled(true);
            updateBackendInv(inventoryClick.player());
            slotActions.getOrDefault(inventoryClick.slot(), universalAction).accept(inventoryClick);
        });
    }

    public abstract String colorize(String s);

    public abstract void updateBackendInv(ProtocolizePlayer player);

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
    public ProtocolizeGUI onClick(Consumer<InventoryClick> consumer) {
        universalAction = consumer;
        return this;
    }

    public ProtocolizeGUI item(int slot, ItemStack item, Consumer<InventoryClick> action){
        item(slot, item);
        slotActions.put(slot, action);
        update();
        return this;
    }

    @Override
    public Inventory item(int slot, ItemStack stack) {
        super.item(slot, stack);
        update();
        return this;
    }

    public ProtocolizeGUI item(int slot, ItemStackBuilder item, Consumer<InventoryClick> action){
        item(slot, (ItemStack) item.build());
        slotActions.put(slot, action);
        update();
        return this;
    }

    public abstract void open(PLAYER p);
    public abstract void close(PLAYER p);

    public void open(UUID uuid){
        ProtocolizePlayer player = Protocolize.playerProvider().player(uuid);
        if(player==null) return;
        player.openInventory(this);
        windowId(getInvId());
        open = true;
        pp = uuid;
    }

    public void close(UUID uuid){
        ProtocolizePlayer player = Protocolize.playerProvider().player(uuid);
        if(player==null) return;
        player.closeInventory();
        open = false;
        pp = null;
    }

    @Override
    public Inventory onClose(Consumer<InventoryClose> consumer) {
        closeConsumer = consumer;
        return this;
    }

    public int getInvId(){
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

    public void reopen(PLAYER player){
        if(!open) return;
        close(player);
        open(player);
    }

    public void update(){
        state++;
        try {
            if(!open) return;
            if(pp==null) return;
            ProtocolizePlayer player = Protocolize.playerProvider().player(pp);
            if(windowId==-1) {
                Logger.info("WindowID not found for GUI of player "+pp.toString());
                return;
            }
            List<BaseItemStack> items = new ArrayList<>(Lists.newArrayList(itemsIndexed(player.protocolVersion())));
            player.sendPacket(new WindowItems((short) windowId, items, state));
        } catch (Throwable t){
            t.printStackTrace();
        }
    }
}
