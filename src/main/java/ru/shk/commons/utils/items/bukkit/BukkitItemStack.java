package ru.shk.commons.utils.items.bukkit;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import land.shield.playerapi.CachedPlayer;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import ru.shk.commons.Commons;
import ru.shk.commons.utils.CustomHead;
import ru.shk.commons.utils.items.ItemStackBuilder;
import ru.shk.commons.utils.items.universal.EnchantmentType;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;

public class BukkitItemStack extends ItemStackBuilder<ItemStack, Material> {
    private int customHeadId = -1;
    private ItemStack item;

    public BukkitItemStack() {
        item = new ItemStack(Material.AIR);
    }

    public BukkitItemStack(ItemStack itemStack) {
        item = itemStack;
    }

    public BukkitItemStack(Material item) {
        type(item);
    }

    public BukkitItemStack(String type) {
        type(type);
    }

    @Override
    public ItemStackBuilder<ItemStack, Material> customHeadId(int id) {
        base64head(Commons.getInstance().getCustomHeadTexture(id));
        this.customHeadId = id;
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, Material> customHeadId(String key) {
        CustomHead h = Commons.getInstance().findCustomHead(key);
        if(h==null) return null;
        base64head(h.getTexture());
        this.customHeadId = h.getId();
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, Material> displayName(String name) {
        item.editMeta(meta -> meta.setDisplayName(name));
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, Material> customModelData(int id) {
        item.editMeta(meta -> meta.setCustomModelData(id));
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, Material> leatherColor(Color color) {
        item.editMeta(meta -> ((LeatherArmorMeta)meta).setColor(org.bukkit.Color.fromBGR(color.getRed(), color.getGreen(), color.getBlue())));
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, Material> leatherColor(String hexColor) {
        Color color = Color.decode(hexColor);
        item.editMeta(meta -> ((LeatherArmorMeta)meta).setColor(org.bukkit.Color.fromBGR(color.getRed(), color.getGreen(), color.getBlue())));
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, Material> type(Material material) {
        if(customHeadId!=-1 && item!=null && (item.getType()==Material.PLAYER_HEAD || item.getType()==Material.PLAYER_WALL_HEAD) && !(material==Material.PLAYER_HEAD || material==Material.PLAYER_WALL_HEAD)) customHeadId = -1;
        if(item==null){
            item = new ItemStack(material);
        } else {
            item.setType(material);
        }
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, Material> type(String material) {
        return type(Material.valueOf(material.toUpperCase()));
    }

    @Override
    public ItemStackBuilder<ItemStack, Material> lore(List<String> lore) {
        item.editMeta(meta -> meta.setLore(lore.stream().map(this::colorize).toList()));
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, Material> unbreakable(boolean b) {
        item.editMeta(meta -> meta.setUnbreakable(b));
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, Material> enchant(EnchantmentType e, int level) {
        item.addEnchantment(Enchantment.getByKey(NamespacedKey.minecraft(e.namespacedKey())), level);
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, Material> flags(int flags) {
        List<ru.shk.commons.utils.items.universal.ItemFlag> f = ru.shk.commons.utils.items.universal.ItemFlag.fromInt(flags);
        item.editMeta(meta -> f.forEach(itemFlag -> meta.addItemFlags(ItemFlag.valueOf(itemFlag.bukkitName()))));
        return this;
    }

    public ItemStackBuilder<ItemStack, Material> enchant(Enchantment e, int level){
        item.addUnsafeEnchantment(e, level);
        return this;
    }

    public ItemStackBuilder<ItemStack, Material> bukkitFlags(ItemFlag... flags){
        item.editMeta(meta -> {
            for (ItemFlag flag : flags) meta.addItemFlags(flag);
        });
        return this;
    }
    public ItemStackBuilder<ItemStack, Material> bukkitFlags(List<ItemFlag> flags){
        item.editMeta(meta -> {
            for (ItemFlag flag : flags) meta.addItemFlags(flag);
        });
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, Material> amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, Material> damage(int damage) {
        item.editMeta(meta -> ((Damageable)meta).setDamage(damage));
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, Material> headOwner(String name) {
        return headOwner(Bukkit.getOfflinePlayer(name));
    }

    public ItemStackBuilder<ItemStack, Material> headOwner(OfflinePlayer p) {
        this.customHeadId = -1;
        item.editMeta(meta -> ((SkullMeta)meta).setOwningPlayer(p));
        return this;
    }

    @Override
    public ItemStackBuilder<ItemStack, Material> headOwner(UUID uuid) {
        return headOwner(Bukkit.getOfflinePlayer(uuid));
    }

    @Override
    public ItemStackBuilder<ItemStack, Material> headOwner(CachedPlayer player) {
        return headOwner(player.getUuid());
    }

    @Override
    public ItemStackBuilder<ItemStack, Material> base64head(String base64) {
        SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
        GameProfile profile = new GameProfile(new UUID(0,0), "");
        profile.getProperties().put("textures", new Property("textures", base64));
        Field profileField;
        try {
            profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, profile);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
        }
        this.customHeadId = -1;
        item.setItemMeta(skullMeta);
        return this;
    }

    @Override
    public int customHeadId() {
        return customHeadId;
    }

    @Override
    public String displayName() {
        return item.getItemMeta().getDisplayName();
    }

    @Override
    public int customModelData() {
        return 0;
    }

    @Override
    public Color leatherColor() {
        org.bukkit.Color c = ((LeatherArmorMeta)item.getItemMeta()).getColor();
        return new Color(c.getRed(), c.getGreen(), c.getBlue());
    }

    @Override
    public String leatherColorAsHexString() {
        org.bukkit.Color c = ((LeatherArmorMeta)item.getItemMeta()).getColor();
        return rgbToHex(c.getRed(), c.getGreen(), c.getBlue());
    }

    @Override
    public Material type() {
        return item.getType();
    }

    @Override
    public List<String> lore() {
        return item.getItemMeta().getLore();
    }

    @Override
    public boolean isUnbreakable() {
        return item.getItemMeta().isUnbreakable();
    }

    @Override
    public List<ru.shk.commons.utils.items.universal.Enchantment> enchantments() {
        List<ru.shk.commons.utils.items.universal.Enchantment> enchantments = new ArrayList<>();
        item.getEnchantments().forEach((enchantment, integer) -> enchantments.add(new ru.shk.commons.utils.items.universal.Enchantment(EnchantmentType.fromString(enchantment.getKey().getKey()), integer)));
        return enchantments;
    }

    public List<Pair<Enchantment, Integer>> enchantmentsAsBukkit() {
        List<Pair<Enchantment, Integer>> enchantments = new ArrayList<>();
        item.getEnchantments().forEach((enchantment, integer) -> enchantments.add(Pair.of(enchantment, integer)));
        return enchantments;
    }

    @Override
    public List<ru.shk.commons.utils.items.universal.ItemFlag> flags() {
        return item.getItemFlags().stream().map(flag -> ru.shk.commons.utils.items.universal.ItemFlag.fromBukkit(flag.name())).toList();
    }

    public List<ItemFlag> flagsAsBukkit() {
        return item.getItemFlags().stream().toList();
    }

    @Override
    public int amount() {
        return item.getAmount();
    }

    @Override
    public int damage() {
        return ((Damageable)item.getItemMeta()).getDamage();
    }

    @Override
    public String headOwnerName() {
        OfflinePlayer p = ((SkullMeta)item.getItemMeta()).getOwningPlayer();
        return p==null?null:p.getName();
    }

    @Override
    public UUID headOwnerUUID() {
        OfflinePlayer p = ((SkullMeta)item.getItemMeta()).getOwningPlayer();
        return p==null?null:p.getUniqueId();
    }

    @Override
    public String base64head() {
        SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
        try {
            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            GameProfile profile = (GameProfile) profileField.get(skullMeta);
            Collection<Property> collection = profile.getProperties().get("textures");
            return collection.stream().filter(property -> property.getName().equals("textures")).findAny().get().getValue();
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException ignored) {}
        this.customHeadId = -1;
        return null;
    }

    @Override
    public ItemStack build() {
        return item;
    }
}
