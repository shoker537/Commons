package ru.shk.commons.utils.items.bungee;

import dev.simplix.protocolize.api.item.ItemStack;
import dev.simplix.protocolize.data.ItemType;
import land.shield.playerapi.CachedPlayer;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.querz.nbt.tag.*;
import org.apache.commons.lang3.tuple.Pair;
import ru.shk.commons.utils.CustomHead;
import ru.shk.commons.utils.items.ItemStackBuilder;
import ru.shk.commons.utils.items.universal.Enchantment;
import ru.shk.commons.utils.items.universal.EnchantmentType;
import ru.shk.commons.utils.items.universal.ItemFlag;
import ru.shk.commonsbungee.Commons;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BungeeItemStack extends ItemStackBuilder<ItemStack, ItemType> {
    private int customHeadId = -1;
    private ItemStack item;

    public BungeeItemStack() {
        item = new ItemStack(ItemType.AIR);
    }

    public BungeeItemStack(@NonNull ItemStack stack) {
        this.item = stack;
    }

    public BungeeItemStack(@NonNull ItemType item) {
        this.item = new ItemStack(item);
    }

    public BungeeItemStack(@NonNull String type) {
        type(type);
    }

    @Override
    public ItemStackBuilder<ItemStack, ItemType> customHeadId(int id) {
        base64head(Commons.getInstance().getCustomHeadTexture(id));
        this.customHeadId = id;
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, ItemType> customHeadId(String key) {
        CustomHead h = Commons.getInstance().findCustomHead(key);
        if(h==null) return this;
        base64head(h.getTexture());
        this.customHeadId = h.getId();
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, ItemType> displayName(String name) {
        item.displayName(stringToComponent(colorize(name)));
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, ItemType> customModelData(int id) {
        item.nbtData().put("CustomModelData", new IntTag(id));
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, ItemType> leatherColor(Color color) {
        CompoundTag tag = item.nbtData().getCompoundTag("display");
        tag.put("color", new IntTag(color.getRGB()));
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, ItemType> leatherColor(String hexColor) {
        Color color = Color.decode(hexColor);
        CompoundTag tag = item.nbtData().getCompoundTag("display");
        tag.put("color", new IntTag(color.getRGB()));
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, ItemType> type(ItemType type) {
        item.itemType(type);
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, ItemType> type(String material) {
        item.itemType(ItemType.valueOf(material.toUpperCase()));
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, ItemType> lore(List<String> lore) {
        item.lore(stringsToComponentList(lore.stream().map(this::colorize).toList()), false);
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, ItemType> unbreakable(boolean b) {
        item.nbtData().put("Unbreakable", new ByteTag((byte)(b?1:0)));
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, ItemType> enchant(EnchantmentType e, int level) {
        ListTag<?> array = item.nbtData().getListTag("Enchantments");
        for (int i = 0; i < array.size(); i++) {
            CompoundTag enchantment = (CompoundTag) array.get(i);
            String id = enchantment.getString("id");
            if(!id.equals(e.namespacedKey())) continue;
            int l = enchantment.getInt("lvl");
            if(l!=level) enchantment.put("lvl", new IntTag(level));
            return this;
        }
        ListTag<CompoundTag> newList = new ListTag<>(CompoundTag.class);
        array.forEach(tag -> newList.add((CompoundTag) tag));
        CompoundTag eTag = new CompoundTag();
        eTag.put("id", new StringTag("minecraft:"+e.namespacedKey()));
        eTag.put("lvl", new IntTag(level));
        newList.add(eTag);
        item.nbtData().put("Enchantments", newList);
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, ItemType> flags(int flags) {
        item.nbtData().put("HideFlags", new IntTag(flags));
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, ItemType> amount(int amount) {
        item.amount((byte) amount);
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, ItemType> damage(int damage) {
        item.nbtData().put("Damage", new IntTag(damage));
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, ItemType> headOwner(String name) {
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, ItemType> headOwner(UUID uuid) {
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, ItemType> headOwner(CachedPlayer player) {
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, ItemType> base64head(String base64) {
        return this;
    }

    @Override
    public int customHeadId() {
        return customHeadId;
    }

    @Override
    public String displayName() {
        return item.displayName(true);
    }

    @Override
    public int customModelData() {
        return item.nbtData().containsKey("CustomModelData")?item.nbtData().getInt("CustomModelData"):-1;
    }

    @Override
    public Color leatherColor() {
        if(!item.nbtData().containsKey("display") || !item.nbtData().getCompoundTag("display").containsKey("color")) return null;
        int color = item.nbtData().getCompoundTag("display").getInt("color");
        return new Color(color);
    }

    @Override
    public String leatherColorAsHexString() {
        Color c = leatherColor();
        if(c==null) return null;
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    @Override
    public ItemType type() {
        return item.itemType();
    }

    @Override
    public List<String> lore() {
        return item.lore(true);
    }

    @Override
    public boolean isUnbreakable() {
        return item.nbtData().containsKey("Unbreakable") && item.nbtData().getByte("Unbreakable")==1;
    }

    @Override
    public List<Enchantment> enchantments() {
        List<Enchantment> list = new ArrayList<>();
        ListTag<?> array = item.nbtData().getListTag("Enchantments");
        array.forEach(tag -> {
            CompoundTag enchantment = (CompoundTag) tag;
            String id = enchantment.getString("id");
            int l = enchantment.getInt("lvl");
            list.add(new Enchantment(EnchantmentType.fromString(id), l));
        });
        return list;
    }

    @Override
    public List<ItemFlag> flags() {
        if(!item.nbtData().containsKey("HideFlags")) return new ArrayList<>();
        return ItemFlag.fromInt(item.nbtData().getInt("HideFlags"));
    }

    @Override
    public int amount() {
        return item.amount();
    }

    @Override
    public int damage() {
        return item.nbtData().containsKey("Damage")?item.nbtData().getInt("Damage"):0;
    }

    @Override
    public String headOwnerName() {
        return null;
    }

    @Override
    public UUID headOwnerUUID() {
        return null;
    }

    @Override
    public String base64head() {
        return null;
    }

    @Override
    public ItemStack build() {
        return item;
    }

    public static Object stringToComponent(String s){
        return new BaseComponent[]{new TextComponent(s)};
    }

    public static List<Object> stringsToComponentList(List<String> list){
        return list.stream().map(s -> ChatColor.WHITE+s).map(ru.shk.commonsbungee.ItemStackBuilder::stringToComponent).toList();
    }
}
