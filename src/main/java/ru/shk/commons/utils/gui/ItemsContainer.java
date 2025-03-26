package ru.shk.commons.utils.gui;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import ru.shk.commons.utils.items.ItemStackBuilder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Accessors(fluent = true)
@Getter
public abstract class ItemsContainer<T extends ItemsContainer> {
    private final GUI.GUIType type;
    private final ConcurrentHashMap<Integer, Item> items;
    @Setter private int lines = 6;

    public ItemsContainer(GUI.GUIType type){
        items = new ConcurrentHashMap<>();
        this.type = type;
    }

    public T clear(int slot){
        items.remove(slot);
        return (T) this;
    }

    public T item(int slot, @Nullable ItemStackBuilder stack) {
        checkSlotBounds(slot);
        if(stack==null) {
            clear(slot);
        } else {
            items.put(slot, new Item(stack, null));
        }
        return (T) this;
    }

    public T item(int slot, @NonNull ItemStackBuilder stack, Consumer<ClickEvent> onClick) {
        return item(slot, stack, onClick, false);
    }

    public T item(int slot, @NonNull ItemStackBuilder stack, Consumer<ClickEvent> onClick, boolean runAsync) {
        checkSlotBounds(slot);
        items.put(slot, new Item(stack, onClick, runAsync));
        return (T) this;
    }

    private void checkSlotBounds(int slot){
        int max;
        if(type== GUI.GUIType.CHEST) {
            max = lines * 9;
        } else {
            max = type.maxSlots();
        }
        if(slot<0 || slot >= max) throw new IllegalArgumentException("Slot "+slot+" does not exist in "+type.name()+" gui with "+max+" slots");
    }

    public abstract GUI parent();

}
