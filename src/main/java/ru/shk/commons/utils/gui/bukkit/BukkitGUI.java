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

import java.util.UUID;
import java.util.function.Consumer;

public class BukkitGUI extends GUI<BukkitGUI> {
    private Inventory inventory = null;

    public BukkitGUI(Object plugin, GUIType type, Player player, Component title) {
        super(plugin, type, player, title);
    }

    @Override
    public void open() {
        super.open();
        if (inventory==null) {
            if(type()==GUIType.CHEST) {
                inventory = Bukkit.createInventory(null, lines()*9, title());
            } else {
                inventory = Bukkit.createInventory(null, typeAsBukkit(type()), title());
            }
        }
        refillInv();
        player().openInventory(inventory);
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
        player().closeInventory();
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
        for (int i = 0; i < items().length; i++) {
            Item item = items()[i];
            inventory.setItem(i, (ItemStack) item.stack().build());
        }
    }

    @Override
    public UUID playerUUID() {
        return player().getUniqueId();
    }

    @Override
    public void title(Component title) {
        inventory = null;
        reopen();
    }

    @Override
    public void sync(Runnable r) {
        Commons.getInstance().sync(r);
    }

    @Override
    public void async(Runnable r) {
        Commons.getInstance().async(r);
    }

    private static InventoryType typeAsBukkit(GUIType type) {
        return switch (type) {
            case ANVIL -> InventoryType.ANVIL;
            case CHEST -> InventoryType.CHEST;
        };
    }
}
