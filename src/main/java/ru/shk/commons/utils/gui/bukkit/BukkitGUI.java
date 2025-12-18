package ru.shk.commons.utils.gui.bukkit;

import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.shk.commons.Commons;
import ru.shk.commons.utils.gui.ClickEvent;
import ru.shk.commons.utils.gui.GUI;
import ru.shk.commons.utils.gui.Item;
import ru.shk.commons.utils.items.bukkit.BukkitItemStack;
import ru.shk.commons.utils.runnables.Schedule;

import java.util.UUID;
import java.util.function.Consumer;

public class BukkitGUI extends GUI<BukkitGUI> {
    private Inventory inventory = null;

    public BukkitGUI(Object plugin, GUIType type, Player player, Component title) {
        super(plugin, type, player, title);
    }

    @Override
    public void open() {
        if(Bukkit.isPrimaryThread()) doOpen(); else Commons.getInstance().sync(this::doOpen);
    }

    private void doOpen(){
        if (inventory==null) {
            if(type()==GUIType.CHEST) {
                inventory = Bukkit.createInventory(null, lines()*9, title());
            } else {
                inventory = Bukkit.createInventory(null, typeAsBukkit(type()), title());
            }
        }
        refillInv();
        player().openInventory(inventory);
        super.open();
    }

    public Player player(){
        return (Player) super.player();
    }

    public BukkitGUI item(int slot, ItemStack item) {
        return item(slot, new BukkitItemStack(item));
    }

    public BukkitGUI item(int slot, @NonNull ItemStack item, Consumer<ClickEvent> onClick) {
        return item(slot, new BukkitItemStack(item), onClick);
    }

    @Override
    public void close() {
        if(Bukkit.isPrimaryThread()) player().closeInventory(); else Commons.getInstance().sync(() -> player().closeInventory());
    }

    @Override
    public boolean isThisInventory(Object inventory) {
        return inventory.equals(this.inventory);
    }

    @Override
    public void updateLocalInv() {
        player().updateInventory();
    }

    @Override
    public void refillInv() {
        if (Bukkit.isPrimaryThread()) doRefillInv(); else Schedule.sync(this::doRefillInv);
    }

    private void doRefillInv(){
        if (inventory==null) return;
        int max = type()==GUIType.CHEST?lines()*9:type().maxSlots();
        for (int i = 0; i < max; i++) {
            Item item = items().get(i);
            if(item==null){
                inventory.setItem(i, null);
            } else {
                inventory.setItem(i, (ItemStack) item.stack().build());
            }
        }
    }

    @Override
    public UUID playerUUID() {
        return player().getUniqueId();
    }

    @Override
    public BukkitGUI title(Component title) {
        super.title(title);
        inventory = null;
        reopen();
        return this;
    }

    private static InventoryType typeAsBukkit(GUIType type) {
        return switch (type) {
            case ANVIL -> InventoryType.ANVIL;
            case CHEST -> InventoryType.CHEST;
        };
    }
}
