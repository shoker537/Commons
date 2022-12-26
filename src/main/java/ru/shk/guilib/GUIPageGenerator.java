package ru.shk.guilib;

import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.shk.commons.Commons;
import ru.shk.commons.utils.ItemStackBuilder;

import java.util.List;
import java.util.function.Function;

@Getter
public class GUIPageGenerator {
    
    private static final Runnable EMPTY_ACTION = () -> {};
    private final int slotOffset;
    @Setter
    private Function<Integer, List<Pair<ItemStack, Runnable>>> pageGenerator;
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
    private final Player viewer;
    private ItemStack leftButton = new ItemStackBuilder(Material.PAPER).customModelData(Commons.getInstance().getConfig().getInt("gui.generator.arrow-left.cmd", 0)).displayName("&b<< Назад").build();
    private ItemStack rightButton = new ItemStackBuilder(Material.PAPER).customModelData(Commons.getInstance().getConfig().getInt("gui.generator.arrow-right.cmd", 0)).displayName("&b>> Дальше").build();
    private ItemStack backgroundItem = new ItemStackBuilder(Material.AIR).build();

    public GUIPageGenerator(Player viewer, GUI gui, int skipLinesCount, int linesCountOfGeneratedItems, ItemStack nothingItem, int nothingSlot, Function<Integer, List<Pair<ItemStack, Runnable>>> pageGenerator, Function<Integer, Boolean> pageExistsCheck, ItemStack fillerItem) {
        this(viewer, gui, skipLinesCount, linesCountOfGeneratedItems, nothingItem, nothingSlot, fillerItem);
        this.pageGenerator = pageGenerator;
        this.pageExistsCheck = pageExistsCheck;
        generatePage();
    }

    public GUIPageGenerator(Player viewer, GUI gui, int skipLinesCount, int linesCountOfGeneratedItems, ItemStack nothingItem, int nothingSlot, ItemStack fillerItem) {
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

    public void setBackgroundItem(ItemStack item) {
        ItemStackBuilder b = new ItemStackBuilder(item);
        if(b.customModelData()<1 && item.getType()!=Material.AIR) {
            b.customModelData(Commons.getInstance().getConfig().getInt("gui.generator.bg-item.cmd", 0));
        }
        this.backgroundItem = b.build();
    }

    public void setLeftButton(ItemStack item) {
        ItemStackBuilder b = new ItemStackBuilder(item);
        if(b.customModelData()<1 && item.getType()!=Material.AIR) {
            b.customModelData(Commons.getInstance().getConfig().getInt("gui.generator.arrow-left.cmd", 0));
        }
        this.leftButton = b.build();
    }

    public void setRightButton(ItemStack item) {
        ItemStackBuilder b = new ItemStackBuilder(item);
        if(b.customModelData()<1 && item.getType()!=Material.AIR) {
            b.customModelData(Commons.getInstance().getConfig().getInt("gui.generator.arrow-right.cmd", 0));
        }
        this.rightButton = b.build();
    }

    public void generatePage(){
        clear();
        addButtonsLine();
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
    }

    public void clear(){
        for (int i = firstGeneratorSlot; i <= lastGeneratorSlot+9; i++) {
            gui.clear(i);
            gui.item(i, backgroundItem);
        }
    }

    public void fillBottomPanes(){
        for (int i = firstButtonsLineSlot; i <= firstButtonsLineSlot+8; i++) gui.item(i, fillerItem);
    }

    public void addButtonsLine(){
        fillBottomPanes();
        boolean previous = pageExistsCheck.apply(currentPage-1);
        boolean next = pageExistsCheck.apply(currentPage+1);
        if(next) gui.item(firstButtonsLineSlot+8, rightButton, () -> switchPage(true));
        if(previous) gui.item(firstButtonsLineSlot, leftButton, () -> switchPage(false));
    }

    public void switchPage(boolean next){
        currentPage += next?1:-1;
        Commons.getInstance().async(this::generatePage);
    }

}
