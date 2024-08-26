package ru.shk.commons.utils.gui.paged;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import ru.shk.commons.utils.gui.ClickEvent;
import ru.shk.commons.utils.gui.GUI;
import ru.shk.commons.utils.gui.Item;
import ru.shk.commons.utils.items.ItemStackBuilder;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

@Setter@Accessors(fluent = true, chain = true)@NoArgsConstructor
public class Paged<ITEM> {
    private int lineStartsAt = 0;
    private int lineEndsAt = 1; // keep in mind that next line (lineEndsAt + 1) will be a service line for buttons
    private Function<ITEM, ItemStackBuilder> itemConverter;
    private BiFunction<Integer, Integer, List<ITEM>> pageGenerator; // page number, items on page, result
    private BiPredicate<Integer, Integer> pageChecker; // page number, items on page
    private BiConsumer<Integer, ITEM> onClickItem;
    private boolean useServiceLine = true;
    private Consumer<OverlayItemsProvider> overlaysGenerator;
    private GUI attachedGUI;

    private ItemStackBuilder prevArrow = ItemStackBuilder.newEmptyStack().type("arrow").displayName("&6< НАЗАД");
    private ItemStackBuilder nextArrow = ItemStackBuilder.newEmptyStack().type("arrow").displayName("&6ВПЕРЕД >");

    public Paged(GUI attachedGUI){
        this.attachedGUI = attachedGUI;
    }

    @Getter(AccessLevel.NONE) private List<ITEM> currentPageItems = new ArrayList<>();
    private int currentPageIndex = 0;

    public List<ITEM> getCurrentPageItems() {
        return new ArrayList<>(currentPageItems);
    }

    public void generate(){
        attachedGUI.async(() -> {
            List<ITEM> currentPageItems = pageGenerator.apply(currentPageIndex, itemsOnPage());
            List<ItemStackBuilder> stacks = convertItemsInParallel(currentPageItems);
            boolean hasLeft = pageChecker.test(currentPageIndex-1, itemsOnPage());
            boolean hasRight = pageChecker.test(currentPageIndex+1, itemsOnPage());
            attachedGUI.sync(() -> {
                this.currentPageItems = currentPageItems;
                if(currentPageItems.isEmpty()) {
                    ItemStackBuilder fill = ItemStackBuilder.newEmptyStack().type("red_stained_glass_pane").displayName("&cНичего не найдено");
                    fillGeneratedArea(fill);
                } else {
                    clearGeneratedArea();
                    int startSlot = lineStartsAt*9;
                    for (int i = 0; i < stacks.size(); i++) {
                        int finalStartSlot = startSlot;
                        int finalI = i;
                        attachedGUI.item(startSlot, stacks.get(i), clickEvent -> clickItem(finalStartSlot, currentPageItems.get(finalI)), true);
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

                    if(hasLeft) overlays.item(0, prevArrow, clickEvent -> prevPage());
                    if(hasRight) overlays.item(8, nextArrow, clickEvent -> nextPage());

                    int overlaysStartIndex = (lineEndsAt+1) * 9;
                    for (int i = 0; i < overlays.items.length; i++) {
                        Item item = overlays.items[i];
                        if(item!=null) {
                            attachedGUI.item(overlaysStartIndex+i, item.stack(), item.onClick());
                        }
                    }
                }
            });
        });
    }

    private List<ItemStackBuilder> convertItemsInParallel(List<ITEM> items){
        HashMap<Integer, ItemStackBuilder> stacksMap = new HashMap<>();
        ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(Math.max(2, items.size()/9));
        for (int i = 0; i < items.size(); i++) {
            int index = i;
            ITEM item = items.get(i);
            pool.submit(() -> {
                ItemStackBuilder result = itemConverter.apply(item);
                synchronized (stacksMap) {
                    stacksMap.put(index, result);
                }
            });
        }
        pool.shutdown();
        try {
            pool.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return Collections.EMPTY_LIST;
        }
        List<ItemStackBuilder> stacks = new ArrayList<>();
        for (int i = 0; i < 54; i++) {
            ItemStackBuilder stack = stacksMap.get(i);
            if(stack==null) break;
            stacks.add(stack);
        }
        return stacks;
    }

    public void clearGeneratedArea(){
        fillGeneratedArea(null);
    }

    public void fillGeneratedArea(@Nullable ItemStackBuilder stack){
        for (int i = lineStartsAt*9; i < (lineEndsAt * 9) + 8; i++) attachedGUI.item(i, stack);
    }

    private void clickItem(int slot, ITEM item){
        if(onClickItem!=null) onClickItem.accept(slot, item);
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
