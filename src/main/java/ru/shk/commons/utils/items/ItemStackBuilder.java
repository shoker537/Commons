package ru.shk.commons.utils.items;

import land.shield.playerapi.CachedPlayer;
import lombok.NoArgsConstructor;
import ru.shk.commons.ServerType;
import ru.shk.commons.utils.items.universal.Enchantment;
import ru.shk.commons.utils.items.universal.EnchantmentType;
import ru.shk.commons.utils.items.universal.ItemFlag;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
@NoArgsConstructor
public abstract class ItemStackBuilder<ITEM,MATERIAL extends Enum> {

    public static ItemStackBuilder newEmptyStack(){
        switch (ServerType.get()){
            case BUNGEE -> {
                return new ru.shk.commons.utils.items.bungee.BungeeItemStack();
            }
            case SPIGOT -> {
                return new ru.shk.commons.utils.items.bukkit.BukkitItemStack();
            }
        }
        throw new RuntimeException("Unsupported ServerType!");
    }

    public ItemStackBuilder(ITEM item) {
        throw new RuntimeException("You have to override this constructor");
    }
    public ItemStackBuilder(MATERIAL item) {
        throw new RuntimeException("You have to override this constructor");
    }
    public ItemStackBuilder(String type) {
        throw new RuntimeException("You have to override this constructor");
    }

    // SETTERS
    public abstract ItemStackBuilder<ITEM,MATERIAL> customHeadId(int id);
    public abstract ItemStackBuilder<ITEM,MATERIAL> customHeadId(String key);
    public abstract ItemStackBuilder<ITEM,MATERIAL> displayName(String name);
    public abstract ItemStackBuilder<ITEM,MATERIAL> customModelData(int id);
    public abstract ItemStackBuilder<ITEM,MATERIAL> leatherColor(Color color);
    public abstract ItemStackBuilder<ITEM,MATERIAL> leatherColor(String hexColor);
    public abstract ItemStackBuilder<ITEM,MATERIAL> type(MATERIAL material);
    public abstract ItemStackBuilder<ITEM,MATERIAL> type(String material);
    public abstract ItemStackBuilder<ITEM,MATERIAL> lore(List<String> lore);
    public abstract ItemStackBuilder<ITEM,MATERIAL> unbreakable(boolean b);
    public abstract ItemStackBuilder<ITEM,MATERIAL> enchant(EnchantmentType e, int level);
    public ItemStackBuilder<ITEM,MATERIAL> enchant(List<Enchantment> enchantments){
        enchantments.forEach(this::enchant);
        return this;
    }
    public ItemStackBuilder<ITEM,MATERIAL> enchant(Enchantment... enchantments){
        for (Enchantment e : enchantments) enchant(e.type(), e.level());
        return this;
    }
    public ItemStackBuilder<ITEM,MATERIAL> allHideFlags(){
        return flags(127);
    }
    /**
     *  Adds all flags from a list, does not override already applied flags.
     *  See ItemStackBuilder#flags(int) to override flags.
     */
    public ItemStackBuilder<ITEM,MATERIAL> flags(List<ItemFlag> flags){
        List<ItemFlag> f = new ArrayList<>();
        for (ItemFlag flag : flags) {
            if(!f.contains(flag)) f.add(flag);
        }
        return flags(ItemFlag.asInt(f));
    }
    /**
     *  Adds all flags from a list, does not override already applied flags.
     *  See ItemStackBuilder#flags(int) to override flags.
     */
    public ItemStackBuilder<ITEM,MATERIAL> flags(ItemFlag... flags){
        List<ItemFlag> f = new ArrayList<>(Arrays.asList(flags));
        return flags(f);
    }
    public abstract ItemStackBuilder<ITEM,MATERIAL> flags(int flags);
    public abstract ItemStackBuilder<ITEM,MATERIAL> amount(int amount);
    public abstract ItemStackBuilder<ITEM,MATERIAL> damage(int damage);
    public abstract ItemStackBuilder<ITEM,MATERIAL> headOwner(String name);
    public abstract ItemStackBuilder<ITEM,MATERIAL> headOwner(UUID uuid);
    public abstract ItemStackBuilder<ITEM,MATERIAL> headOwner(CachedPlayer player);
    public abstract ItemStackBuilder<ITEM,MATERIAL> base64head(String base64);



    // GETTERS
    public abstract int customHeadId();
    public abstract String displayName();
    public abstract int customModelData();
    public abstract Color leatherColor();
    public abstract String leatherColorAsHexString();
    public abstract MATERIAL type();
    public abstract List<String> lore();
    public abstract boolean isUnbreakable();
    public abstract List<Enchantment> enchantments();
    public abstract List<ItemFlag> flags();
    public abstract int amount();
    public abstract int damage();
    public abstract String headOwnerName();
    public abstract UUID headOwnerUUID();
    public abstract String base64head();



    // UTILITIES
    public abstract ITEM build();
    public static String rgbToHex(int r, int g, int b){
        return String.format("#%02X%02X%02X", r, g, b);
    }
    public String colorize(String s){
        switch (ServerType.get()){
            case BUNGEE -> {
                return ru.shk.commonsbungee.Commons.colorizeWithHex(s);
            }
            case SPIGOT -> {
                return ru.shk.commons.Commons.colorizeWithHex(s);
            }
        }
        throw new RuntimeException("Unsupported ServerType!");
    }
}
