package ru.shk.guilibbungee;

import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.inventory.Inventory;
import dev.simplix.protocolize.api.inventory.InventoryClick;
import dev.simplix.protocolize.api.item.ItemStack;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import dev.simplix.protocolize.data.inventory.InventoryType;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import ru.shk.commonsbungee.Commons;

import java.util.HashMap;
import java.util.function.Consumer;

public class GUI extends Inventory {
    private final HashMap<Integer, Runnable> slotActions = new HashMap<>();
    private Consumer<InventoryClick> universalAction = inventoryClick -> inventoryClick.cancelled(true);

    public GUI(String name, InventoryType type) {
        super(type);
        title(Commons.getInstance().colorize(name));
        super.onClick(inventoryClick-> {
            inventoryClick.cancelled(true);
            if(slotActions.containsKey(inventoryClick.slot())){
                slotActions.get(inventoryClick.slot()).run();
                return;
            }
            universalAction.accept(inventoryClick);
        });
    }

    @Override
    public GUI onClick(Consumer<InventoryClick> consumer) {
        universalAction = consumer;
        return this;
    }

    public GUI item(int slot, ItemStack item, Runnable action){
        item(slot, item);
        slotActions.put(slot, action);
        return this;
    }

    public void open(ProxiedPlayer p){
        ProtocolizePlayer player = Protocolize.playerProvider().player(p.getUniqueId());
        player.closeInventory();
        player.openInventory(this);
    }
}
