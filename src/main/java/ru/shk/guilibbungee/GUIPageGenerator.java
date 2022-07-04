package ru.shk.guilibbungee;

import dev.simplix.protocolize.api.inventory.InventoryClick;
import dev.simplix.protocolize.api.item.ItemStack;
import dev.simplix.protocolize.data.ItemType;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.apache.commons.lang3.tuple.Pair;
import ru.shk.commonsbungee.Commons;
import ru.shk.commonsbungee.ItemStackBuilder;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Getter
public class GUIPageGenerator {
    private final int slotOffset;
    @Setter private Function<Integer, List<Pair<ItemStack, Consumer<InventoryClick>>>> pageGenerator;
    @Setter private Function<Integer, Boolean> pageExistsCheck;
    private final GUI gui;
    private final int nothingSlot;
    private final ItemStack nothingItem;
    private final int firstGeneratorSlot;
    private final int countOfGeneratedItems;
    private final int lastGeneratorSlot;
    private final int firstButtonsLineSlot;
    private final ItemStack fillerItem;
    @Setter private int currentPage = 0;
    private final ProxiedPlayer viewer;

    public GUIPageGenerator(ProxiedPlayer viewer, GUI gui, int skipLinesCount, int linesCountOfGeneratedItems, ItemStack nothingItem, int nothingSlot, Function<Integer, List<Pair<ItemStack, Consumer<InventoryClick>>>> pageGenerator, Function<Integer, Boolean> pageExistsCheck, ItemStack fillerItem) {
        this(viewer,gui, skipLinesCount, linesCountOfGeneratedItems, nothingItem, nothingSlot, fillerItem);
        this.pageGenerator = pageGenerator;
        this.pageExistsCheck = pageExistsCheck;
        generatePage();
    }

    public GUIPageGenerator(ProxiedPlayer viewer, GUI gui, int skipLinesCount, int linesCountOfGeneratedItems, ItemStack nothingItem, int nothingSlot, ItemStack fillerItem) {
        this.slotOffset = skipLinesCount*9;
        this.gui = gui;
        this.viewer = viewer;
        this.countOfGeneratedItems = linesCountOfGeneratedItems*9;
        firstGeneratorSlot = skipLinesCount*9;
        this.lastGeneratorSlot = (skipLinesCount+linesCountOfGeneratedItems)*9-1;
        this.nothingItem = nothingItem;
        this.nothingSlot = nothingSlot;
        this.fillerItem = fillerItem;
        firstButtonsLineSlot = lastGeneratorSlot+1;
    }

    public void generatePage(){
        clear();
        if(pageExistsCheck.apply(currentPage)){
            val generated = pageGenerator.apply(currentPage);
            if(generated.size()==0){
                gui.item(nothingSlot, nothingItem);
            } else {
                for (int i = 0; i < generated.size(); i++) {
                    val pair = generated.get(i);
                    gui.item(slotOffset+i, pair.getKey(), pair.getRight());
                }
            }
        } else {
            gui.item(nothingSlot, nothingItem);
        }
        addButtonsLine();
        gui.update();
    }

    public void clear(){
        for (int i = firstGeneratorSlot; i <= lastGeneratorSlot+9; i++) gui.clear(i);
    }

    public void fillBottomPanes(){
        for (int i = firstButtonsLineSlot; i <= firstButtonsLineSlot+8; i++) gui.item(i, fillerItem);
    }

    public void addButtonsLine(){
        fillBottomPanes();
        boolean previous = pageExistsCheck.apply(currentPage-1);
        boolean next = pageExistsCheck.apply(currentPage+1);
        if(next) gui.item(firstButtonsLineSlot+8, new ItemStackBuilder(ItemType.PAPER).displayName("&b>> Дальше").build(), inventoryClick -> switchPage(true));
        if(previous) gui.item(firstButtonsLineSlot, new ItemStackBuilder(ItemType.PAPER).displayName("&b<< Назад").build(), inventoryClick -> switchPage(false));
    }

    public void switchPage(boolean next){
        currentPage += next?1:-1;
        Commons.getInstance().async(this::generatePage);
    }

}
