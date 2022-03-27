package ru.shk.guilib;

import lombok.AccessLevel;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import ru.shk.commons.Commons;
import ru.shk.commons.utils.ItemStackBuilder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
public class GUI {
    private JavaPlugin ownerPlugin;
    private ItemStack emptyFiller = null;
    private final String title;
    private final int slots;
    @Getter(AccessLevel.NONE) private Inventory inv;
    private final HashMap<Integer, ItemStack> items = new HashMap<>();
    private final HashMap<Integer, Runnable> slotActions = new HashMap<>();
    private final HashMap<Material, Runnable> materialActions = new HashMap<>();
    @Getter(AccessLevel.NONE) private TriConsumer<ClickType, Integer, ItemStack> universalAction = null;

    @Deprecated
    public GUI(int slots, String title){
        this.slots = slots;
        this.title = ChatColor.translateAlternateColorCodes('&', title);
    }

    public GUI(JavaPlugin plugin, int slots, String title){
        this.slots = slots;
        this.title = ChatColor.translateAlternateColorCodes('&', title);
        this.ownerPlugin = plugin;
    }

    @Deprecated
    public HashMap<Material, Runnable> getMaterialActions(){
        return materialActions;
    }
    @Deprecated
    public HashMap<Integer, Runnable> getSlotActions(){
        return slotActions;
    }

    public void addMaterialAction(Material m, Runnable action){
        materialActions.put(m, action);
    }

    public GUI addItem(int slot, ItemStack item){
        items.put(slot, item);
        return this;
    }
    public GUI addItem(int slot, ItemStackBuilder item){
        items.put(slot, item.build());
        return this;
    }
    public GUI addItem(int slot, ItemStack item, Runnable clicked){
        items.put(slot, item);
        slotActions.put(slot, clicked);
        return this;
    }
    public GUI addItem(int slot, ItemStackBuilder item, Runnable clicked){
        items.put(slot, item.build());
        slotActions.put(slot, clicked);
        return this;
    }
    public void addSlotAction(int slot, Runnable action){
        slotActions.put(slot, action);
    }
    public GUI fillEmpty(ItemStack item){
        emptyFiller = item;
        return this;
    }
    public GUI withUniversalAction(TriConsumer<ClickType, Integer, ItemStack> consumer){
        universalAction = consumer;
        return this;
    }
    public GUI addAll(List<ItemStack> items){
        for (int i = 0; i < items.size(); i++) addItem(i, items.get(i));
        return this;
    }
    public void clear(int max){
        for (int i = 0; i < max; i++) inv.setItem(i, null);
    }
    public void clear(){
        for (int i = 0; i < slots; i++) inv.setItem(i, null);
    }
    public void close(){
        if(inv==null) throw new IllegalStateException("Inventory is not open yet!");
        new ArrayList<>(inv.getViewers()).forEach(HumanEntity::closeInventory);
    }
    public void clickedSlot(ClickType type, int slot, ItemStack item){
        if(slotActions.size()!=0){
            if(slotActions.containsKey(slot)) {
                slotActions.get(slot).run();
                return;
            }
        }
        if(materialActions.size()!=0){
            if(materialActions.containsKey(item.getType())) {
                materialActions.get(item.getType()).run();
                return;
            }
        }
        if(universalAction!=null) universalAction.accept(type, slot, item);
    }
    public boolean isOpen(){
        return inv!=null && inv.getViewers().size()!=0;
    }
    public void setItemRaw(int slot, ItemStack item){
        if(inv==null) throw new IllegalStateException("Inventory is not open yet!");
        Commons.getInstance().sync(() -> inv.setItem(slot, item));
    }
    @Nullable
    public ItemStack getItem(int slot){
        if(inv==null) throw new IllegalStateException("Inventory is not open yet!");
        return inv.getItem(slot);
    }
    public void open(Player p){
        inv = Bukkit.createInventory(null, slots, title);
        for (int i = 0; i < slots; i++) {
            if(items.containsKey(i)){
                inv.setItem(i, items.get(i));
            } else {
                if(emptyFiller!=null) inv.setItem(i, emptyFiller);
            }
        }
        Commons.getInstance().sync(p::closeInventory);
        Commons.getInstance().syncLater(() -> {
            if(!p.isOnline()) return;
            GUILib.getInstance().getGuis().put(p.getUniqueId(), this);
            p.openInventory(inv);
        }, 1);
    }
}
