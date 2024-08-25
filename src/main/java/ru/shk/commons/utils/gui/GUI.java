package ru.shk.commons.utils.gui;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import ru.shk.commons.ServerType;
import ru.shk.commons.utils.gui.bukkit.BukkitGUI;
import ru.shk.commons.utils.gui.velocity.VelocityGUI;
import ru.shk.commons.utils.items.ItemStackBuilder;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Getter
@Accessors(fluent = true)
public abstract class GUI<G extends GUI> {
    private final Object plugin;
    private final GUIType type;
    private final Object player;
    private final Component title;
    private final Item[] items;
    private Consumer<UniversalClick> universalClick = null;
    private int lines = 1;
    @Getter(AccessLevel.NONE) private boolean isOpen = false;
    @Getter(AccessLevel.NONE) private final AtomicBoolean isInClickProcessing = new AtomicBoolean(false);

    public GUI(Object plugin, GUIType type, Object player, Component title) {
        this.plugin = plugin;
        this.type = type;
        this.player = player;
        this.title = title;
        items = new Item[type.maxSlots];
    }

    public void startClickProcessing(){
        isInClickProcessing.set(true);
    }

    public void endClickProcessing(){
        isInClickProcessing.set(false);
    }

    public boolean isOpen(){
        return isOpen;
    }

    public G lines(int lines) {
        this.lines = lines;
        return (G) this;
    }

    public void open(){
        isOpen = true;
        GUIManager.instance().add(playerUUID(), this);
    }
    public abstract void close();
    public void onClose(){
        isOpen = false;
        GUIManager.instance().removeGUI(playerUUID(), this);
    }
    public abstract boolean isThisInventory(Object inventory);
    public abstract void updateLocalInv();
    public abstract void refillInv();
    public abstract UUID playerUUID();
    public void onClick(UniversalClick click){
        if(isInClickProcessing.get()) return;
        startClickProcessing();
        Item item = items[click.slot()];
        if(item!=null && item.onClick()!=null) {
            if(item.runAsync()) {
                async(() -> {
                    try {
                        item.onClick().accept(new ClickEvent(click.slot(), click.type(), click.shift()));
                    } catch (Throwable t){
                        t.printStackTrace();
                    }
                    endClickProcessing();
                });
            } else {
                try {
                    item.onClick().accept(new ClickEvent(click.slot(), click.type(), click.shift()));
                } catch (Throwable t){
                    t.printStackTrace();
                }
                endClickProcessing();
            }
            return;
        }
        if(universalClick!=null) {
            try {
                universalClick.accept(click);
            } catch (Throwable t){
                t.printStackTrace();
            }
        }
        endClickProcessing();
    }

    public G universalClick(Consumer<UniversalClick> universalClick){
        this.universalClick = universalClick;
        return (G) this;
    }

    public G item(int slot, @Nullable ItemStackBuilder stack) {
        if(stack==null) {
            items[slot] = null;
        } else {
            items[slot] = new Item(stack, null);
        }
        if(isOpen) update();
        return (G) this;
    }

    public G item(int slot, @NonNull ItemStackBuilder stack, Consumer<ClickEvent> onClick) {
        return item(slot, stack, onClick, false);
    }
    public G item(int slot, @NonNull ItemStackBuilder stack, Consumer<ClickEvent> onClick, boolean runAsync) {
        items[slot] = new Item(stack, onClick, runAsync);
        if(isOpen) update();
        return (G) this;
    }

    public void update(){
        refillInv();
        updateLocalInv();
    }

    public enum ClickType {
        LEFT, RIGHT, MIDDLE
    }

    public enum GUIType {
        CHEST(54),
        ANVIL(2);
        private final int maxSlots;

        GUIType(int maxSlots) {
            this.maxSlots = maxSlots;
        }
    }

    public static GUI chest(Object plugin, Object player, Component title, int lines){
        return switch (ServerType.get()) {
            default -> null;
            case SPIGOT -> new BukkitGUI(plugin, GUIType.CHEST, (org.bukkit.entity.Player) player, title).lines(lines);
            case VELOCITY -> new VelocityGUI(plugin, GUIType.CHEST, (com.velocitypowered.api.proxy.Player) player, title).lines(lines);
        };
    }

    public static GUI anvil(Object plugin, Object player, Component title){
        return switch (ServerType.get()) {
            default -> null;
            case SPIGOT -> new BukkitGUI(plugin, GUIType.ANVIL, (org.bukkit.entity.Player) player, title);
            case VELOCITY -> new VelocityGUI(plugin, GUIType.ANVIL, (com.velocitypowered.api.proxy.Player) player, title);
        };
    }
    public abstract void title(Component title);
    public void reopen(){
        close();
        open();
    }

    public abstract void sync(Runnable r);
    public abstract void async(Runnable r);

}
