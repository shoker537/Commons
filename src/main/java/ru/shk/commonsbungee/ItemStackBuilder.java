package ru.shk.commonsbungee;

import dev.simplix.protocolize.api.chat.ChatElement;
import dev.simplix.protocolize.api.item.ItemStack;
import dev.simplix.protocolize.data.ItemType;
import io.netty.util.concurrent.DefaultThreadFactory;
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
import ru.shk.commons.utils.CustomHead;
import ru.shk.configapibungee.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

@Deprecated
public class ItemStackBuilder {
//    private static final ThreadPoolExecutor mojangRequestThreadPool = new ThreadPoolExecutor(1, 3, 10, TimeUnit.SECONDS, new SynchronousQueue<>());
//    private static final ThreadPoolExecutor cacheThreadPool = new ThreadPoolExecutor(2, 5, 60, TimeUnit.SECONDS, new SynchronousQueue<>());
    private static final ThreadPoolExecutor mojangRequestThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1, new DefaultThreadFactory("Commons Mojang Request Pool"));
    private static final ThreadPoolExecutor cacheThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1, new DefaultThreadFactory("Commons Deprecated ItemStackBuilder Cache Pool"));
    private final ItemStack item;
    private static final List<Pair<CachedPlayer, String>> headsCache = new ArrayList<>(300);
    @Getter private int customHeadId = -1;

    public static int headsCacheSize(){return headsCache.size();}

    public ItemStackBuilder(ItemStack stack){
        this.item = stack;
    }

    public ItemStackBuilder(ItemType type){
        item = new ItemStack(type);
    }

    public ItemStackBuilder customHead(int id){
        base64head(Commons.getInstance().getCustomHeadTexture(id));
        this.customHeadId = id;
        return this;
    }

    public ItemStackBuilder customHead(String key){
        CustomHead h = Commons.getInstance().findCustomHead(key);
        if(h==null) return null;
        base64head(h.getTexture());
        this.customHeadId = h.getId();
        return this;
    }

    public ItemStackBuilder(Configuration section){
        ItemType type = ItemType.valueOf(section.getString("type"));
        if(type==ItemType.PLAYER_HEAD) {
            if(section.contains("custom-head-id")){
                Object o = section.get("custom-head-id");
                if(o instanceof Integer i){
                    item = Commons.getInstance().getCustomHead(i).build();
                } else if(o instanceof String s){
                    item = Commons.getInstance().getCustomHead(s).build();
                } else {
                    item = new ItemStack(type);
                    Commons.getInstance().warning("Item has an invalid custom-head-id value: "+o.getClass().getSimpleName()+". Only int/string supported.");
                }
            } else {
                item = new ItemStack(type);
            }
        } else {
            item = new ItemStack(type);
        }
        if(type==ItemType.PLAYER_HEAD){
            Config.getIfHasString(section, "head-owner", this::headOwner);
            Config.getIfHasString(section, "head-base64", this::base64head);
        }
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
        Config.getIfHasInt(section, "amount", this::amount);
        Config.getIfHasInt(section, "custom-model-data", this::customModelData);
    }

    public ItemType type(){
        return item.itemType();
    }

    public ItemStackBuilder type(ItemType type){
        item.itemType(type);
        return this;
    }

    public Integer customModelData(){
        return item.nbtData().containsKey("CustomModelData")?item.nbtData().getInt("CustomModelData"):null;
    }

    public ItemStackBuilder customModelData(int data){
        item.nbtData().put("CustomModelData", new IntTag(data));
        return this;
    }

    public ItemStackBuilder amount(int amount){
        item.amount((byte) amount);
        return this;
    }

    public int amount(){
        return item.amount();
    }

    public ItemStackBuilder lore(List<String> lore){
        List<ChatElement<?>> list = new ArrayList<>();
        lore.forEach(string -> list.add(ChatElement.of(stringToComponent(Commons.colorizeWithHex(string)))));
        item.lore(list);
        return this;
    }

    public ItemStackBuilder lore(String... lore){
        lore(List.of(lore));
        return this;
    }

    public ItemStackBuilder displayName(String name){
        item.displayName(ChatElement.of(stringToComponent(Commons.getInstance().colorize(name))));
        return this;
    }

    public ItemStackBuilder headOwner(String player){
        Optional<Pair<CachedPlayer,String>> o = headsCache.stream().filter(pair -> pair.getLeft().getName().equalsIgnoreCase(player)).findAny();
        if(o.isPresent()) return base64head(o.get().getRight());
        item.nbtData().put("SkullOwner", new StringTag(player));
        customHeadId = -1;
        return this;
    }
    /**
     * Applies player skin texture to PLAYER_HEAD.<br>
     * <b>Can be run in the main thread.</b><br>
     * If a skin texture is not cached, just puts an nbt-tag with player name, which shows Steve first and then after a few seconds gets updated to player skin. Not getting cached this way, loads the skin each time.
     *    **/
    public ItemStackBuilder headOwner(CachedPlayer player){
        Optional<Pair<CachedPlayer,String>> o = headsCache.stream().filter(pair -> pair.getLeft().getId()==player.getId()).findAny();
        if(o.isPresent()) return base64head(o.get().getRight(), player.getName());
        item.nbtData().put("SkullOwner", new StringTag(player.getName()));
        customHeadId = -1;
        return this;
    }

    public String headOwner(){
        Tag<?> tag = item.nbtData().get("SkullOwner");
        if(tag instanceof StringTag) return ((StringTag)tag).getValue();
        @Nullable CompoundTag skullOwnerTag = item.nbtData().getCompoundTag("SkullOwner");
        return skullOwnerTag.getString("Name");
    }
    /**
     * Applies player skin texture to PLAYER_HEAD.<br>
     * <b>Note: Never run in the main thread!</b><br>
     * If a skin texture is not cached, makes request to Mojang web API.
     *    **/
    public ItemStackBuilder headOwnerBlockingThread(CachedPlayer player){
        String texture = getPlayerHead(player);
        if(texture==null) return headOwner(player.getName());
        customHeadId = -1;
        return base64head(texture, player.getName());
    }

    /**
     * The most right way to get player head in asynchronous way.<br>
     * If a skin texture is not cached, searches in database, or if there's no cached texture makes request to Mojang web API.
     *    **/
    public static void getPlayerHead(CachedPlayer player, Consumer<ItemStackBuilder> consumer){
        ItemStackBuilder b = new ItemStackBuilder(ItemType.PLAYER_HEAD);
        cacheThreadPool.submit(() -> {
            Optional<Pair<CachedPlayer, String>> o = headsCache.stream().filter(pair -> pair.getLeft().getId()==player.getId()).findAny();
            if(o.isEmpty()){
                mojangRequestThreadPool.submit(() -> {
                    String texture = Commons.getInstance().getSkinTexture(player);
                    if(texture==null) {
                        consumer.accept(b.headOwner(player.getName()));
                        return;
                    }
                    headsCache.add(Pair.of(player, texture));
                    while (headsCache.size()>300) headsCache.remove(0);
                    consumer.accept(b.base64head(texture));
                });
                return;
            }
            consumer.accept(b.base64head(o.get().getRight()));
        });
    }

    private static String getPlayerHead(CachedPlayer player){
        Optional<Pair<CachedPlayer, String>> o = headsCache.stream().filter(pair -> pair.getLeft().getId()==player.getId()).findAny();
        if(o.isEmpty()){
            String texture = Commons.getInstance().getSkinTexture(player);
            if(texture==null) return null;
            headsCache.add(Pair.of(player, texture));
            while (headsCache.size()>300) headsCache.remove(0);
            return texture;
        }
        return o.get().getRight();
    }

    public String base64head(){
        final @NotNull CompoundTag tag = item.nbtData();
        @Nullable CompoundTag skullOwnerTag = tag.getCompoundTag("SkullOwner");
        if(skullOwnerTag==null) return null;
        @Nullable CompoundTag propertiesTag = tag.getCompoundTag("Properties");
        if(propertiesTag==null) return null;
        if(!propertiesTag.containsKey("textures")) return null;
        final @NotNull ListTag<@NotNull CompoundTag> texturesTag = (ListTag<CompoundTag>) propertiesTag.getListTag("textures");
        final @NotNull CompoundTag textureTag = texturesTag.get(0);
        return textureTag.getString("Value");
    }

    public ItemStackBuilder base64head(String texture){
        customHeadId = -1;
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
        skullOwnerTag.put("Name", new StringTag("##aboba"));

        tag.put("SkullOwner", skullOwnerTag);

        tag.put("HideFlags", new IntTag(99));
        tag.put("overrideMeta", new ByteTag((byte)1));
        item.nbtData(tag);
        return this;
    }

    public ItemStackBuilder base64head(String texture, String ownerName){
        customHeadId = -1;
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
        skullOwnerTag.put("Name", new StringTag(ownerName));

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

    public static Object stringToComponent(String s){
        return new BaseComponent[]{new TextComponent(s)};
    }

    public static List<Object> stringsToComponentList(List<String> list){
        return list.stream().map(s -> ChatColor.WHITE+s).map(ItemStackBuilder::stringToComponent).toList();
    }

    @Override
    public String toString() {
        return ItemStackConverter.toString(this);
    }
}
