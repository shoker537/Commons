package ru.shk.guilib.protocolize;

import com.google.common.collect.Lists;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.chat.ChatElement;
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
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.shk.commons.utils.Logger;
import ru.shk.commons.utils.items.ItemStackBuilder;
import ru.shk.commons.utils.items.bungee.BungeeItemStack;

import java.util.*;
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
    private int state = -1;
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

    public ProtocolizeGUI titleJson(String json){
        title(ChatElement.ofJson(json));
        return this;
    }

    public ProtocolizeGUI title(Object title, boolean string){
        title(string?ChatElement.ofLegacyText((String) title):ChatElement.of(title));
        return this;
    }
    public abstract void removeGuiFromCache(UUID playerUUID);

    public ProtocolizeGUI(PLUGIN plugin, String name, InventoryType type, boolean autoColorizeTitle){
        this(plugin, name, type, autoColorizeTitle, false);
    }

    public ProtocolizeGUI(PLUGIN plugin, String name, InventoryType type, boolean autoColorizeTitle, boolean jsonTitle){
        super(type);
        this.owner = plugin;
        String colorizedTitle = autoColorizeTitle?colorize(name):name;
        if(jsonTitle) title(ChatElement.ofJson(colorizedTitle)); else title(ChatElement.ofLegacyText(colorizedTitle));
        super.onClick(inventoryClick-> {
            inventoryClick.cancelled(true);
            updateBackendInv(inventoryClick.player());
            slotActions.getOrDefault(inventoryClick.slot(), universalAction).accept(inventoryClick);
        });
    }

    public abstract String colorize(String s);

    public abstract void updateBackendInv(ProtocolizePlayer player);

    public void clear(int itemSlot){
        clear(itemSlot, true);
    }

    public void clear(int itemSlot, boolean update){
        removeItem(itemSlot);
        super.item(itemSlot, new ItemStack(ItemType.AIR));
        slotActions.remove(itemSlot);
        if(update) update();
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
    public Inventory item(int slot, @NonNull ItemStack stack) {
        super.item(slot, stack);
        update();
        return this;
    }

    public Inventory item(int slot, ItemStackBuilder stack) {
        super.item(slot, ((BungeeItemStack)stack).build());
        update();
        return this;
    }

    public ProtocolizeGUI item(int slot, ItemStackBuilder item, Consumer<InventoryClick> action){
        item(slot, (ItemStack) item.build());
        slotActions.put(slot, action);
        update();
        return this;
    }

    public void open(PLAYER p){
        state++;
    }
    public abstract void close(PLAYER p);

    public void open(UUID uuid){
        ProtocolizePlayer player = Protocolize.playerProvider().player(uuid);
        if(player==null) return;
        player.openInventory(this);
        open = true;
        pp = uuid;
        windowId(getInvId());
    }

    public void close(UUID uuid){
        ProtocolizePlayer player = Protocolize.playerProvider().player(uuid);
        if(player==null) return;
        try {
            player.closeInventory();
        } catch (Throwable t){
            t.printStackTrace();
        }
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
        if(pp==null) return windowId;
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
//        if(!open) return;
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
//            List<BaseItemStack> items = new ArrayList<>();
//            var all = itemsIndexed(player.protocolVersion());
//            Logger.warning("WindowItems packet with "+all.size()+" items");
//            for (BaseItemStack baseItemStack : all) {
//                if (items.size()==54) {
//                    break;
//                }
//                items.add(new ItemStack(baseItemStack.itemType()));
//            }
            player.sendPacket(new WindowItems(windowId, items, state));
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ProtocolizeGUI<?, ?> that = (ProtocolizeGUI<?, ?>) o;
        return open == that.open && state == that.state && windowId == that.windowId && Objects.equals(slotActions, that.slotActions) && Objects.equals(universalAction, that.universalAction) && Objects.equals(closeConsumer, that.closeConsumer) && Objects.equals(owner, that.owner) && Objects.equals(pp, that.pp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), slotActions, universalAction, closeConsumer, owner, pp, open, state, windowId);
    }
}
