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
    @Getter private static final Consumer<InventoryClick> EMPTY_ACTION = click -> {};
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
    @Getter private final ProxiedPlayer viewer;
    private ItemStack leftButton = new ItemStackBuilder(ItemType.PAPER).customModelData(Commons.getInstance().getConfig().getInt("gui.generator.arrow-left.cmd", 0)).displayName("&b<< Назад").build();
    private ItemStack rightButton = new ItemStackBuilder(ItemType.PAPER).customModelData(Commons.getInstance().getConfig().getInt("gui.generator.arrow-right.cmd", 0)).displayName("&b>> Дальше").build();
    private ItemStack backgroundItem = new ItemStackBuilder(ItemType.AIR).customModelData(Commons.getInstance().getConfig().getInt("gui.generator.bg-item.cmd", 0)).build();

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

    public void setBackgroundItem(ItemStack backgroundItem) {
        ItemStackBuilder b = new ItemStackBuilder(backgroundItem);
        if(b.customModelData()==null) {
            b.customModelData(Commons.getInstance().getConfig().getInt("gui.generator.bg-item.cmd", 0));
        }
        this.backgroundItem = b.build();
    }

    public void setLeftButton(ItemStack leftButton) {
        ItemStackBuilder b = new ItemStackBuilder(leftButton);
        if(b.customModelData()==null) {
            b.customModelData(Commons.getInstance().getConfig().getInt("gui.generator.arrow-left.cmd", 0));
        }
        this.leftButton = b.build();
    }

    public void setRightButton(ItemStack rightButton) {
        ItemStackBuilder b = new ItemStackBuilder(rightButton);
        if(b.customModelData()==null) {
            b.customModelData(Commons.getInstance().getConfig().getInt("gui.generator.arrow-right.cmd", 0));
        }
        this.rightButton = b.build();
    }

    public void generatePage(){
        clear();
        addButtonsLine();
        gui.update();
        if(pageExistsCheck.apply(currentPage)){
            val generated = pageGenerator.apply(currentPage);
            if(generated.size()==0){
                gui.item(nothingSlot, nothingItem);
            } else {
                for (int i = 0; i < generated.size(); i++) {
                    val pair = generated.get(i);
                    gui.item(slotOffset+i, pair.getKey(), pair.getRight()==null?EMPTY_ACTION:pair.getRight());
                }
            }
        } else {
            gui.item(nothingSlot, nothingItem);
        }
        gui.update();
    }

    public void clear(){
        for (int i = firstGeneratorSlot; i <= lastGeneratorSlot+9; i++) {
            gui.clear(i, false);
            if(backgroundItem!=null && backgroundItem.itemType()!=ItemType.AIR) gui.item(i, backgroundItem);
        }
        gui.update();
    }

    public void fillBottomPanes(){
        for (int i = firstButtonsLineSlot; i <= firstButtonsLineSlot+8; i++) gui.item(i, fillerItem);
    }

    public void addButtonsLine(){
        fillBottomPanes();
        boolean previous = pageExistsCheck.apply(currentPage-1);
        boolean next = pageExistsCheck.apply(currentPage+1);
        if(next) gui.item(firstButtonsLineSlot+8, rightButton, inventoryClick -> switchPage(true));
        if(previous) gui.item(firstButtonsLineSlot, leftButton, inventoryClick -> switchPage(false));
    }

    public void switchPage(boolean next){
        currentPage += next?1:-1;
        Commons.getInstance().async(this::generatePage);
    }

}
