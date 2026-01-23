package ru.shk.commons.utils.gui.paged;

import lombok.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import ru.shk.commons.ServerType;
import ru.shk.commons.utils.Logger;
import ru.shk.commons.utils.gui.ClickEvent;
import ru.shk.commons.utils.gui.GUI;
import ru.shk.commons.utils.gui.Item;
import ru.shk.commons.utils.gui.ItemsContainer;
import ru.shk.commons.utils.items.ItemStackBuilder;
import ru.shk.commons.utils.runnables.Schedule;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;

@Setter@Accessors(fluent = true, chain = true)@NoArgsConstructor
public class Paged<ITEM> {
    private int lineStartsAt = 0;
    private int lineEndsAt = 1; // keep in mind that next line (lineEndsAt + 1) will be a service line for buttons
    private Function<ITEM, ItemStackBuilder> itemConverter;
    private BiFunction<Integer, Integer, List<ITEM>> pageGenerator; // page number, items on page, result
    private BiPredicate<Integer, Integer> pageChecker; // page number, items on page
    private BiConsumer<Integer, ITEM> onClickItem;
    private BiConsumer<Integer, ITEM> onShiftClickItem;
    private BiConsumer<ClickEvent, ITEM> onUniversalClickItem;
    private boolean useServiceLine = true;
    private Consumer<OverlayItemsProvider> overlaysGenerator;
    private BiFunction<ITEM, ItemStackBuilder, ItemStackBuilder> delayedGenerate;
    private ItemsContainer<?> attachedGUI;
    private int notFoundItemSlot = -1;
    private ItemStackBuilder notFoundItem = null;
    private final AtomicBoolean pageLoading = new AtomicBoolean();
    private boolean asyncClicks = ServerType.get()!=ServerType.SPIGOT;
    private Offsets offsets = new Offsets(0, 0);

    private ItemStackBuilder prevArrow = ItemStackBuilder.newEmptyStack().type("arrow").displayName("&6< НАЗАД");
    private ItemStackBuilder nextArrow = ItemStackBuilder.newEmptyStack().type("arrow").displayName("&6ВПЕРЕД >");

    public Paged(ItemsContainer attachedGUI){
        this.attachedGUI = attachedGUI;
    }
    public Paged(GUI attachedGUI){
        this.attachedGUI = attachedGUI;
    }

    public record Offsets(int left, int right){}

    public Paged<ITEM> attachedGUI(GUI gui){
        attachedGUI = gui;
        return this;
    }

    public Paged<ITEM> lines(int lines){
        lineEndsAt = lineStartsAt + lines;
        return this;
    }

    public Paged<ITEM> items(@NonNull List<ITEM> items){
        pageGenerator = (number, limit) -> items.stream().skip(number*limit).limit(limit).toList();
        pageChecker = (page, limit) -> page >= 0 && page*limit<=items.size();
        return this;
    }

    @Getter(AccessLevel.NONE) private List<ITEM> currentPageItems = new ArrayList<>();
    private int currentPageIndex = 0;

    public List<ITEM> getCurrentPageItems() {
        return new ArrayList<>(currentPageItems);
    }
    public void generate(){
        generate(false);
    }


    public void generate(boolean goAsync) {
        if (this.notFoundItemSlot == -1) {
            this.notFoundItemSlot = 9 * this.lineStartsAt
                    + (this.lineEndsAt - this.lineStartsAt + 1) / 2 * 9
                    + this.offsets.left()
                    + (9 - this.offsets.left() - this.offsets.right()) / 2;
        }

        Runnable r = () -> {
            if (!this.isPageLoading()) {
                this.lockPageLoading();
                boolean var11 = false;

                try {
                    var11 = true;
                    this.clearGeneratedArea();
                    this.attachedGUI.item(this.notFoundItemSlot, ItemStackBuilder.newEmptyStack().type("clock").displayName("&6Загрузка..."));
                    List<ITEM> currentPageItems = this.pageGenerator.apply(this.currentPageIndex, this.itemsOnPage());
                    boolean var2x = this.pageChecker.test(this.currentPageIndex - 1, this.itemsOnPage());
                    boolean var3 = this.pageChecker.test(this.currentPageIndex + 1, this.itemsOnPage());
                    this.currentPageItems = currentPageItems;
                    List<IndexedItem<ITEM>> var4 = this.convertItemsInParallel(currentPageItems);
                    this.attachedGUI.clear(this.notFoundItemSlot);
                    this.placeItems(var4);

                    if(delayedGenerate!=null){
                        for (IndexedItem<ITEM> stack : var4) {
                            IndexedItem<ITEM> oldItem = stack;
                            Schedule.async(() -> {
                                ItemStackBuilder b = this.delayedGenerate.apply(stack.item, stack.stack.get());
                                stack.stack.set(b);
                                Schedule.sync(() -> {
                                    int slot = stack.index.get();
                                    Item currentItem = this.attachedGUI.items().get(slot);
                                    if (currentItem != null && currentItem.stack() == oldItem.stack.get()) {
                                        this.attachedGUI.item(slot, b, clickEvent -> clickItem(clickEvent, oldItem.item));
                                    }
                                });
                            });
                        }
                    }

                    if (this.useServiceLine) {
                        OverlayItemsProvider overlays = new OverlayItemsProvider();
                        if (this.overlaysGenerator != null) {
                            this.overlaysGenerator.accept(overlays);
                        }

                        if (var2x) {
                            overlays.item(0, this.prevArrow, var1xx -> {
                                Schedule.async(this::prevPage);
                            });
                        }

                        if (var3) {
                            overlays.item(8, this.nextArrow, var1xx -> {
                                Schedule.async(this::nextPage);
                            });
                        }

                        int overlaysStartIndex = (this.lineEndsAt + 1) * 9;

                        for (int i = 0; i < overlays.items.length; i++) {
                            Item item = overlays.items[i];
                            this.attachedGUI
                                    .item(overlaysStartIndex + i, item == null ? ItemStackBuilder.newEmptyStack() : item.stack(), item == null ? null : item.onClick());
                        }

                        var11 = false;
                    } else {
                        var11 = false;
                    }
                } finally {
                    if (var11) {
                        this.releasePageLoading();
                    }
                }

                this.releasePageLoading();
            }
        };
        if (goAsync) {
            Schedule.async(r);
        } else {
            r.run();
        }
    }

    private void placeItems(List<IndexedItem<ITEM>> indexedItems) {
        if (indexedItems.isEmpty()) {
            if (this.notFoundItem != null) {
                this.attachedGUI.item(this.notFoundItemSlot, this.notFoundItem);
            }
        } else {
            int columns = 9 - this.offsets.left() - this.offsets.right();
            int slot = this.lineStartsAt * 9 + this.offsets.left();

            for (int i = 0; i < indexedItems.size(); i++) {
                int finalI = i;
                IndexedItem<ITEM> indexedItem = indexedItems.get(i);
                indexedItem.index.set(slot);
                this.attachedGUI.item(slot, indexedItem.stack.get(), var3x -> {
                    this.clickItem(var3x, indexedItems.get(finalI).item);
                }, this.asyncClicks);
                slot++;
                if ((i + 1) % columns == 0) {
                    slot += this.offsets.left() + this.offsets.right();
                }
            }
        }
    }

    private record IndexedItem<ITEM>(AtomicInteger index, ITEM item, AtomicReference<ItemStackBuilder> stack){}

    private List<IndexedItem<ITEM>> convertItemsInParallel(List<ITEM> items){
        List<IndexedItem<ITEM>> indexed = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) indexed.add(new IndexedItem<>(new AtomicInteger(i), items.get(i), new AtomicReference<>(null)));
        Consumer<IndexedItem<ITEM>> action = indexedItem -> {
            ItemStackBuilder result;
            try {
                result = itemConverter.apply(indexedItem.item);
            } catch (Throwable t){
                t.printStackTrace();
                result = ItemStackBuilder.newEmptyStack().type("barrier").displayName("&cОшибка предмета");
            }
            indexedItem.stack.set(result);
        };
        if (items.size()<10) {
            for (IndexedItem<ITEM> itemIndexedItem : indexed) action.accept(itemIndexedItem);
            return indexed;
        }
        final ThreadPoolExecutor itemsConverterPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(Math.min(indexed.size()/5, 5));
        for (IndexedItem<ITEM> itemIndexedItem : indexed) {
            itemsConverterPool.submit(() -> action.accept(itemIndexedItem));
        }
        itemsConverterPool.shutdown();
        try {
            itemsConverterPool.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return indexed;
        }
        return indexed.stream().sorted(Comparator.comparingInt(value -> value.index.get())).limit(54).toList();
    }

    public void clearGeneratedArea(){
        fillGeneratedArea(null);
    }

    public void fillGeneratedArea(@Nullable ItemStackBuilder stack){
        for (int i = lineStartsAt*9; i < (lineEndsAt * 9) + 9; i++) {
            if (stack==null) attachedGUI.clear(i); else attachedGUI.item(i, stack);
        }
    }

    private void clickItem(ClickEvent click, ITEM item){
        if (onUniversalClickItem!=null) {
            onUniversalClickItem.accept(click, item);
        } else {
            boolean shift = click.shift();
            int slot = click.slot();
            if (shift && onShiftClickItem!=null) {
                onShiftClickItem.accept(slot, item);
            } else {
                if(onClickItem!=null) onClickItem.accept(slot, item);
            }
        }
    }

    private int itemsOnPage() {
        int columns = 9 - this.offsets.left() - this.offsets.right();
        return (this.lineEndsAt - this.lineStartsAt + 1) * columns;
    }

    public void nextPage(){
        currentPageIndex++;
        generate();
    }

    public void prevPage(){
        currentPageIndex--;
        generate();
    }

    public synchronized void lockPageLoading(){
        pageLoading.set(true);
    }

    public synchronized void releasePageLoading(){
        pageLoading.set(false);
    }

    public synchronized boolean isPageLoading(){
        return pageLoading.get();
    }

    public static class OverlayItemsProvider {
        private final Item[] items = new Item[9];

        public OverlayItemsProvider item(int slot, ItemStackBuilder item, Consumer<ClickEvent> onClick) {
            items[slot] = new Item(item, onClick);
            return this;
        }
    }
}
