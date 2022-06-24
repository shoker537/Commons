package ru.shk.guilib;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import ru.shk.commons.Commons;
import ru.shk.commons.utils.ItemStackBuilder;
import ru.shk.commons.utils.Plugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

public class GUILib implements Plugin, Listener {
    @Getter private static GUILib instance;
    @Getter private final HashMap<UUID, GUI> guis = new HashMap<>();
    private static final GUI waitingGui = new GUI(45, "&6Загрузка...");

    static {
        waitingGui.addItem(22, new ItemStackBuilder(Material.CLOCK).displayName("&5Идёт загрузка").lore(" ", "&fПока загружаются данные,", "&fможешь посмотреть на эти часики", " "));
    }

    public static void openWaitingGui(Player p){
        waitingGui.open(p);
    }

    @Override
    public void load() {
        instance = this;
    }

    @Override
    public void enable() {
        Commons.registerEvents(Commons.getInstance(), this);
    }

    @EventHandler
    public void pluginDisableEvent(PluginDisableEvent e){
        guis.values().stream().filter(gui -> gui.getOwnerPlugin()!=null && gui.getOwnerPlugin().equals(e.getPlugin())).forEach(GUI::close);
    }

    @Override
    public void disable() {
        guis.forEach((uuid, gui) -> {
            Player p = Bukkit.getPlayer(uuid);
            if(p!=null) p.closeInventory();
        });
    }
    @EventHandler
    public void onCloseInventory(InventoryCloseEvent e){
        if(e.getInventory().getType()==InventoryType.CRAFTING) return;
        guis.remove(e.getPlayer().getUniqueId());
    }
    @EventHandler
    public void onQuit(PlayerQuitEvent e){
        guis.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e){
        if(e.getCurrentItem()==null || !guis.containsKey(e.getWhoClicked().getUniqueId()) || e.getClickedInventory()==null) return;
        e.setCancelled(true);
        guis.get(e.getWhoClicked().getUniqueId()).clickedSlot(e.getClick(), e.getSlot(), e.getCurrentItem());
    }
}
