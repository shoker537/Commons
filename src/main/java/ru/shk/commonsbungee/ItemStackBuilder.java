package ru.shk.commonsbungee;

import dev.simplix.protocolize.api.item.ItemStack;
import dev.simplix.protocolize.data.ItemType;
import land.shield.playerapi.CachedPlayer;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.config.Configuration;
import net.querz.nbt.tag.*;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.shk.configapibungee.Config;

import java.util.*;

public class ItemStackBuilder {
    private final ItemStack item;
    private static final List<Pair<Integer, String>> headsCache = new ArrayList<>();

    public ItemStackBuilder(ItemStack stack){
        this.item = stack;
    }

    public ItemStackBuilder(ItemType type){
        item = new ItemStack(type);
    }

    public ItemStackBuilder(Configuration section){
        ItemType type = ItemType.valueOf(section.getString("type"));
        item = new ItemStack(type);
        Config.getIfHasString(section, "display-name", this::displayName);
        Config.getIfHasStringList(section, "lore", this::lore);
        Config.getIfHasStringList(section, "enchant", list -> {
            for (String line : list) {
                String[] parts = line.split(" ");
                String e = parts[0].toUpperCase();
                int level = Integer.parseInt(parts[1]);
                this.enchant(e, level);
            }
        });
        if(type==ItemType.PLAYER_HEAD) {
            Config.getIfHasString(section, "head-owner", this::headOwner);
            Config.getIfHasString(section, "head-base64", this::base64head);
        }
        Config.getIfHasInt(section, "amount", this::amount);
    }

    public ItemStackBuilder amount(int amount){
        item.amount((byte) amount);
        return this;
    }

    public ItemStackBuilder lore(List<String> lore){
        item.lore(stringsToComponentList(lore.stream().map(s -> Commons.getInstance().colorize(s)).toList()), false);
        return this;
    }

    public ItemStackBuilder lore(String... lore){
        item.lore(stringsToComponentList(Arrays.stream(lore).map(s -> Commons.getInstance().colorize(s)).toList()), false);
        return this;
    }

    public ItemStackBuilder displayName(String name){
        item.displayName(stringToComponent(Commons.getInstance().colorize(name)));
        return this;
    }
    @Deprecated
    public ItemStackBuilder headOwner(String player){
        item.nbtData().put("SkullOwner", new StringTag(player));
        return this;
    }
    /**
     * Applies player skin texture to PLAYER_HEAD.<br>
     * <b>Can be run in the main thread.</b><br>
     * If a skin texture is not cached, just puts an nbt-tag with player name, which shows Steve first and then after a few seconds gets updated to player skin. Not getting cached this way, loads the skin each time.
     *    **/
    public ItemStackBuilder headOwner(CachedPlayer player){
        Optional<Pair<Integer,String>> o = headsCache.stream().filter(pair -> pair.getLeft()==player.getId()).findAny();
        if(o.isPresent()) return base64head(o.get().getRight());
        item.nbtData().put("SkullOwner", new StringTag(player.getName()));

        return this;
    }
    /**
     * Applies player skin texture to PLAYER_HEAD.<br>
     * <b>Note: Never run in the main thread!</b><br>
     * If a skin texture is not cached, makes request to Mojang web API.
     *    **/
    public ItemStackBuilder headOwnerBlockingThread(CachedPlayer player){
        Optional<Pair<Integer,String>> o = headsCache.stream().filter(pair -> pair.getLeft()==player.getId()).findAny();
        if(o.isEmpty()) return headOwner(player.getName());
        return base64head(o.get().getRight());
    }

    public ItemStackBuilder base64head(String texture){
        final @NotNull CompoundTag tag = item.nbtData();
        @Nullable CompoundTag skullOwnerTag = tag.getCompoundTag("SkullOwner");
        @Nullable CompoundTag propertiesTag = tag.getCompoundTag("Properties");
        final @NotNull ListTag<@NotNull CompoundTag> texturesTag = new ListTag<>(CompoundTag.class);
        final @NotNull CompoundTag textureTag = new CompoundTag();

        if (skullOwnerTag == null) {
            skullOwnerTag = new CompoundTag();
        }
        if (propertiesTag == null) {
            propertiesTag = new CompoundTag();
        }

        textureTag.put("Value", new StringTag(texture));
        texturesTag.add(textureTag);
        propertiesTag.put("textures", texturesTag);
        skullOwnerTag.put("Properties", propertiesTag);
        skullOwnerTag.put("Name", new StringTag("aboba"));

        tag.put("SkullOwner", skullOwnerTag);

        tag.put("HideFlags", new IntTag(99));
        tag.put("overrideMeta", new ByteTag((byte)1));
        item.nbtData(tag);
        return this;
    }

    public ItemStackBuilder enchant(String enchantment, int level){
        final @NotNull CompoundTag tag = item.nbtData();
        ListTag<CompoundTag> enchantments = (ListTag<CompoundTag>) tag.getListTag("Enchantments");
        if(enchantments==null) enchantments = (ListTag<CompoundTag>) ListTag.createUnchecked(CompoundTag.class);
        CompoundTag e = new CompoundTag();
        e.put("id", new StringTag("minecraft:"+enchantments));
        e.put("lvl", new ShortTag((short) level));
        enchantments.add(e);
        tag.put("Enchantments", enchantments);
        item.nbtData(tag);
        return this;
    }

    public ItemStack build(){
        return item;
    }

    private Object stringToComponent(String s){
        return new BaseComponent[]{new TextComponent(ChatColor.WHITE+s)};
    }

    private List<Object> stringsToComponentList(List<String> list){
        return list.stream().map(this::stringToComponent).toList();
    }

}
