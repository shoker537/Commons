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
import ru.shk.commons.utils.items.TicksDuration;
import ru.shk.commons.utils.runnables.Schedule;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Getter
@Accessors(fluent = true)
public abstract class GUI<G extends GUI> extends ItemsContainer<G> {
    private final Object plugin;
    private final Object player;
    @Setter private Component title;
    private Consumer<UniversalClick> universalClick = null;
    @Getter(AccessLevel.NONE) private boolean isOpen = false; // todo: atomic?
    @Getter(AccessLevel.NONE) private final AtomicBoolean isInClickProcessing = new AtomicBoolean(false);
    private Runnable onTick;

    public GUI(Object plugin, GUIType type, Object player, Component title) {
        super(type);
        this.plugin = plugin;
        this.player = player;
        this.title = title;
    }

    @Override
    public G parent() {
        return (G) this;
    }

    public void doTick(){
        if(onTick!=null) {
            try {
                onTick.run();
            } catch (Throwable t){
                t.printStackTrace();
            }
        }
    }

    public G onTick(Runnable onTick) {
        this.onTick = onTick;
        return (G) this;
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
        super.lines(lines);
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
        Item item = items().get(click.slot());
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

    @Override
    public G clear(int slot) {
        super.clear(slot);
        if(isOpen) refillInv();
        return (G) this;
    }

    @Override
    public G item(int slot, @Nullable ItemStackBuilder stack) {
        super.item(slot, stack);
        if(isOpen) refillInv();
        return (G) this;
    }

    @Override
    public G item(int slot, @NonNull ItemStackBuilder stack, Consumer<ClickEvent> onClick) {
        item(slot, stack, onClick, false);
        return (G) this;
    }

    @Override
    public G item(int slot, @NonNull ItemStackBuilder stack, Consumer<ClickEvent> onClick, boolean runAsync) {
        super.item(slot, stack, onClick, runAsync);
        if(isOpen) refillInv();
        return (G) this;
    }

    public void update(){
        refillInv();
        updateLocalInv();
    }

    public enum ClickType {
        LEFT, RIGHT, MIDDLE
    }

    @Getter@Accessors(fluent = true) public enum GUIType {
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
    public void reopen(){
        close();
        open();
    }

    public void sync(Runnable r){
        Schedule.sync(r);
    }
    public void async(Runnable r){
        Schedule.async(r);
    }

}
