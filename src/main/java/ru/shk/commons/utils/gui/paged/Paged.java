package ru.shk.commons.utils.gui.paged;

import lombok.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
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

    private ItemStackBuilder prevArrow = ItemStackBuilder.newEmptyStack().type("arrow").displayName("&6< НАЗАД");
    private ItemStackBuilder nextArrow = ItemStackBuilder.newEmptyStack().type("arrow").displayName("&6ВПЕРЕД >");

    public Paged(ItemsContainer attachedGUI){
        this.attachedGUI = attachedGUI;
    }
    public Paged(GUI attachedGUI){
        this.attachedGUI = attachedGUI;
    }

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

    public void generate(boolean goAsync){
        if(notFoundItem!=null && notFoundItemSlot==-1) notFoundItemSlot = (9 * lineStartsAt) + (((lineEndsAt - lineStartsAt + 1) / 2)*9) + 4;

        Runnable r = () -> {
            List<ITEM> currentPageItems = pageGenerator.apply(currentPageIndex, itemsOnPage());
            List<IndexedItem<ITEM>> stacks = convertItemsInParallel(currentPageItems);
            boolean hasLeft = pageChecker.test(currentPageIndex-1, itemsOnPage());
            boolean hasRight = pageChecker.test(currentPageIndex+1, itemsOnPage());
            this.currentPageItems = currentPageItems;
            clearGeneratedArea();
            if(currentPageItems.isEmpty()) {
                if(notFoundItem!=null) attachedGUI.item(notFoundItemSlot, notFoundItem);
            } else {
                int startSlot = lineStartsAt*9;
                for (int i = 0; i < stacks.size(); i++) {
                    int finalI = i;
                    attachedGUI.item(startSlot, stacks.get(i).stack.get(), clickEvent -> clickItem(clickEvent, currentPageItems.get(finalI)), true);
                    startSlot++;
                }
            }
            if (useServiceLine) {
                OverlayItemsProvider overlays = new OverlayItemsProvider();
                if(overlaysGenerator!=null) {
                    try {
                        overlaysGenerator.accept(overlays);
                    } catch (Throwable t){
                        t.printStackTrace();
                    }
                }

                if(hasLeft) overlays.item(0, prevArrow, clickEvent -> Schedule.async(this::prevPage));
                if(hasRight) overlays.item(8, nextArrow, clickEvent -> Schedule.async(this::nextPage));

                int overlaysStartIndex = (lineEndsAt+1) * 9;
                for (int i = 0; i < overlays.items.length; i++) {
                    Item item = overlays.items[i];
                    if(item!=null) {
                        attachedGUI.item(overlaysStartIndex+i, item.stack(), item.onClick());
                    } else {
                        attachedGUI.item(overlaysStartIndex+i, null);
                    }
                }
            }
            if (delayedGenerate!=null){
                AtomicInteger pageSaved = new AtomicInteger(currentPageIndex);
                int startSlot = lineStartsAt*9;
                for (IndexedItem<ITEM> item : stacks) {
                    Schedule.async(() -> {
                        ItemStackBuilder result = delayedGenerate.apply(item.item, item.stack.get());
                        Schedule.sync(() -> {
                            Item currentItem = attachedGUI.items().get(startSlot+item.index);
                            Consumer<ClickEvent> click = null;
                            if (currentItem!=null) click = currentItem.onClick();
                            if (pageSaved.get()==currentPageIndex) {
                                attachedGUI.item(startSlot+item.index, result, click);
                            }
                        });
                    });
                }
            }
        };

        if (goAsync) Schedule.async(r); else r.run();
    }

    private record IndexedItem<ITEM>(int index, ITEM item, AtomicReference<ItemStackBuilder> stack){}


    private List<IndexedItem<ITEM>> convertItemsInParallel(List<ITEM> items){
//        final HashMap<Integer, ItemStackBuilder> stacksMap = new HashMap<>();
        List<IndexedItem<ITEM>> indexed = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) indexed.add(new IndexedItem<>(i, items.get(i), new AtomicReference<>(null)));
        Consumer<IndexedItem<ITEM>> action = indexedItem -> {
            ItemStackBuilder result;
            try {
                result = itemConverter.apply(indexedItem.item);
            } catch (Throwable t){
                t.printStackTrace();
                result = ItemStackBuilder.newEmptyStack();
            }
            indexedItem.stack.set(result);
        };
        if (items.size()<5) {
            for (IndexedItem<ITEM> itemIndexedItem : indexed) action.accept(itemIndexedItem);
            return indexed;
        }
        final ThreadPoolExecutor itemsConverterPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(Math.min(indexed.size(), 8));
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
//        indexed.parallelStream().forEach(indexedItem -> {

//            synchronized (stacksMap) {
//                stacksMap.put(indexedItem.index, result);
//            }
//        });
//        List<ItemStackBuilder> stacks = new ArrayList<>();
//        for (int i = 0; i < 54; i++) {
//            ItemStackBuilder stack = stacksMap.get(i);
//            if(stack==null) break;
//            stacks.add(stack);
//        }
        return indexed.stream().sorted(Comparator.comparingInt(value -> value.index)).limit(54).toList();
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

    private int itemsOnPage(){
        return (lineEndsAt-lineStartsAt+1) * 9;
    }

    public void nextPage(){
        currentPageIndex++;
        generate();
    }

    public void prevPage(){
        currentPageIndex--;
        generate();
    }

    public static class OverlayItemsProvider {
        private final Item[] items = new Item[9];

        public OverlayItemsProvider item(int slot, ItemStackBuilder item, Consumer<ClickEvent> onClick) {
            items[slot] = new Item(item, onClick);
            return this;
        }
    }
}
