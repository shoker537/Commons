package ru.shk.commons.utils;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import ru.shk.commons.utils.gui.GUI;
import ru.shk.commons.utils.gui.GUIManager;
import ru.shk.commons.utils.gui.UniversalClick;
import ru.shk.commons.utils.items.bukkit.BukkitItemStack;

public class Events implements Listener {
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e){
        if(e.getDamager().getType()!=EntityType.FIREWORK_ROCKET) return;
        if(e.getDamager().hasMetadata("effect")) e.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        GUI customGUI = GUIManager.instance().customGUI(e.getView().getTopInventory());
        if(customGUI==null || e.getCurrentItem()==null) return;
        e.setCancelled(true);
        if(e.getClickedInventory().equals(e.getView().getBottomInventory())) return;
        GUI.ClickType type = switch (e.getClick()) {
            case LEFT, SHIFT_LEFT -> GUI.ClickType.LEFT;
            case RIGHT, SHIFT_RIGHT -> GUI.ClickType.RIGHT;
            case MIDDLE -> GUI.ClickType.MIDDLE;
            default -> null;
        };
        if(type==null) return;
        customGUI.onClick(new UniversalClick(e.getSlot(), new BukkitItemStack(e.getCurrentItem()), type, e.isShiftClick()));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e){
        GUI customGUI = GUIManager.instance().customGUI(e.getView().getTopInventory());
        if(customGUI==null) return;
        customGUI.onClose();
    }
}