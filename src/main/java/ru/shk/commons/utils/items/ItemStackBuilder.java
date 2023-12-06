package ru.shk.commons.utils.items;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import ru.shk.commons.ServerType;
import ru.shk.commons.utils.items.universal.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
public abstract class ItemStackBuilder<ITEM,MATERIAL extends Enum, R extends ItemStackBuilder> {
    @Getter@Accessors(fluent = true)
    private static final HeadsCache headsCache = new HeadsCache();

    public static ItemStackBuilder newEmptyStack(){
        switch (ServerType.get()){
            case BUNGEE -> {
                return new ru.shk.commons.utils.items.bungee.BungeeItemStack();
            }
            case SPIGOT -> {
                return new ru.shk.commons.utils.items.bukkit.BukkitItemStack();
            }
            case VELOCITY -> {
                return new ru.shk.commons.utils.items.velocity.VelocityItemStack();
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
    public abstract R customHead(int id);
    public abstract R customHead(String key);
    public abstract R displayName(String name);
    public abstract R displayName(Object name);
    public abstract R customModelData(int id);
    public abstract R leatherColor(Color color);
    public abstract R leatherColor(String hexColor);
    public abstract R potionColor(int rgb);
    public abstract R type(MATERIAL material);
    public abstract R type(String material);
    public R lore(String... lore){
        return lore(Arrays.asList(lore));
    }
    public abstract R lore(List<?> lore);
    public abstract R lore(List<String> lore, boolean minimessage);
    public abstract R unbreakable(boolean b);
    public abstract R enchant(EnchantmentType e, int level);
    public R enchant(List<Enchantment> enchantments){
        enchantments.forEach(this::enchant);
        return (R) this;
    }
    public R enchant(Enchantment... enchantments){
        for (Enchantment e : enchantments) enchant(e.type(), e.level());
        return (R) this;
    }
    public R allHideFlags(){
        return flags(127);
    }
    /**
     *  Adds all flags from a list, does not override already applied flags.
     *  See ItemStackBuilder#flags(int) to override flags.
     */
    public R flags(List<ItemFlag> flags){
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
    public R flags(ItemFlag... flags){
        List<ItemFlag> f = new ArrayList<>(Arrays.asList(flags));
        return (R) flags(f);
    }

    public abstract R flags(int flags);
    public abstract R customHeadId(int id);
    public abstract R amount(int amount);
    public abstract R damage(int damage);
    public R headOwner(long playerId){
        String texture = headsCache.getPlayerTexture(playerId);
        if(texture==null) return (R) this;
        return base64head(texture);
    }
    public R headOwner(String name){
        String texture = headsCache.getPlayerTexture(name);
        if(texture==null) return localHeadOwner(name);
        return base64head(texture);
    }
    public R headOwner(UUID uuid){
        String texture = headsCache.getPlayerTexture(uuid);
        if(texture==null) return localHeadOwner(uuid);
        return base64head(texture);
    }
    public abstract R localHeadOwner(String name);
    public abstract R localHeadOwner(UUID uuid);
//    public abstract R headOwner(CachedPlayer player);
    public abstract R base64head(String base64);
    public abstract R potionData(PotionData potionData);
    public abstract R customPotion(PotionEffect potionEffect);

    // GETTERS
    public abstract int customHeadId();
    public abstract Integer potionColor();
    public abstract String displayName();
    public abstract Integer customModelData();
    public abstract Color leatherColor();
    public abstract String leatherColorAsHexString();
    public abstract MATERIAL type();
    public abstract List<String> lore();
    public abstract boolean isUnbreakable();
    public abstract List<Enchantment> enchantments();
    public abstract List<ItemFlag> flags();
    public abstract int amount();
    public abstract Integer damage();
    public abstract String headOwnerName();
    public abstract String base64head();
    public abstract String potionData();
    public abstract String customPotion();


    // UTILITIES
    public abstract R clone();
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
            case VELOCITY -> {
                return s;
            }
        }
        throw new RuntimeException("Unsupported ServerType!");
    }

    public static ItemStackBuilder fromString(String s){
        return ItemStackConverter.fromString(s);
    }

    @Override
    public String toString() {
        return ItemStackConverter.toString(this);
    }
}
