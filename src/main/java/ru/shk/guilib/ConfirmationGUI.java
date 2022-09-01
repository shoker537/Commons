package ru.shk.guilib;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.shk.commons.utils.ItemStackBuilder;

public class ConfirmationGUI extends GUI {
    private String yesName = "Да";
    private String noName = "Нет";
    private Material yesItem = Material.LIME_CONCRETE;
    private Material noItem = Material.RED_CONCRETE;
    private Runnable whenNo = this::close;
    private Runnable whenYes = this::close;

    public ConfirmationGUI(JavaPlugin plugin, String question) {
        super(plugin, 27, question);
    }
    public ConfirmationGUI yesName(String yesName){
        this.yesName = yesName;
        return this;
    }
    public ConfirmationGUI noName(String noName){
        this.noName = noName;
        return this;
    }
    public ConfirmationGUI yesItem(Material yesItem){
        this.yesItem = yesItem;
        return this;
    }
    public ConfirmationGUI noItem(Material noItem){
        this.noItem = noItem;
        return this;
    }
    public ConfirmationGUI whenNo(Runnable whenNo){
        this.whenNo = whenNo;
        return this;
    }
    public ConfirmationGUI whenYes(Runnable whenYes){
        this.whenYes = whenYes;
        return this;
    }

    @Override
    public void open(Player p) {
        addItem(12, new ItemStackBuilder(noItem).displayName(noName).disableFlags(), whenNo);
        addItem(14, new ItemStackBuilder(yesItem).displayName(yesName).disableFlags(), whenYes);
        super.open(p);
    }
}
