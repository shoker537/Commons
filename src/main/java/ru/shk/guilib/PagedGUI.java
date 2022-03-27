package ru.shk.guilib;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.shk.commons.utils.ItemStackBuilder;

import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class PagedGUI extends GUI {
    private final Player p;
    private final Function<Integer, HashMap<Integer, ItemStack>> generator;
    private final Supplier<Integer> maxPagesSupplier;
    private int page;

    public PagedGUI(int slots, String title, Player p, Function<Integer, HashMap<Integer, ItemStack>> generator, Supplier<Integer> maxPagesSupplier) {
        this(slots, title, p, 1, generator, maxPagesSupplier);
    }

    public PagedGUI(int slots, String title, Player p, int currentPage, Function<Integer, HashMap<Integer, ItemStack>> generator, Supplier<Integer> maxPagesSupplier) {
        super(slots, title);
        this.p = p;
        this.maxPagesSupplier = maxPagesSupplier;
        this.generator = generator;
        this.page = currentPage;
        generate();
    }

    private void generate(){
        clear();
        getSlotActions().clear();
        getMaterialActions().clear();
        int maxPages = maxPagesSupplier.get();
        if(page!=1) {
            setItemRaw(getSlots()-8, new ItemStackBuilder(Material.PAPER).displayName(ChatColor.AQUA+"<< НАЗАД").build());
            addSlotAction(getSlots()-8, this::next);
        }
        if(maxPages!=page) {
            setItemRaw(getSlots()-1, new ItemStackBuilder(Material.PAPER).displayName(ChatColor.AQUA+"ДАЛЬШЕ >>").build());
            addSlotAction(getSlots()-1, this::next);
        }
        open(p);
    }

    private void previous(){

    }

    private void next(){

    }

}
