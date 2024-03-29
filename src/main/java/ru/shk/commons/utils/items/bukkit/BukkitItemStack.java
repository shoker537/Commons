package ru.shk.commons.utils.items.bukkit;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
//import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
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
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import ru.shk.commons.Commons;
import ru.shk.commons.utils.CustomHead;
import ru.shk.commons.utils.items.ItemStackBuilder;
import ru.shk.commons.utils.items.ItemStackConverter;
import ru.shk.commons.utils.items.universal.EnchantmentType;
import ru.shk.commons.utils.items.universal.PotionData;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class BukkitItemStack extends ItemStackBuilder<ItemStack, Material, BukkitItemStack> {
    private int customHeadId = -1;
    private ItemStack item;

    public BukkitItemStack() {
        item = new ItemStack(Material.AIR);
    }

    public BukkitItemStack(ItemStack itemStack) {
        item = itemStack;
        if(itemStack.getType()!=Material.AIR && !itemStack.hasItemMeta()) item.setItemMeta(Bukkit.getItemFactory().getItemMeta(item.getType()));
    }

    public BukkitItemStack(Material item) {
        type(item);
    }

    public BukkitItemStack(String type) {
        type(type);
    }

    @Override
    public BukkitItemStack customHead(int id) {
        base64head(Commons.getInstance().getCustomHeadTexture(id));
        this.customHeadId = id;
        return this;
    }

    @Override
    public BukkitItemStack customHead(String key) {
        CustomHead h = Commons.getInstance().findCustomHead(key);
        if(h==null) return null;
        base64head(h.getTexture());
        this.customHeadId = h.getId();
        return this;
    }

    @Override
    public BukkitItemStack displayName(String name) {
        item.editMeta(meta -> meta.setDisplayName(colorize(name)));
        return this;
    }

    @Override
    public BukkitItemStack displayName(Object name) {
        item.editMeta(meta -> meta.displayName((Component) name));
        return this;
    }

    @Override
    public BukkitItemStack customModelData(int id) {
        item.editMeta(meta -> meta.setCustomModelData(id));
        return this;
    }

    @Override
    public BukkitItemStack leatherColor(Color color) {
        item.editMeta(meta -> ((LeatherArmorMeta)meta).setColor(org.bukkit.Color.fromRGB(color.getRed(), color.getGreen(), color.getBlue())));
        return this;
    }

    @Override
    public BukkitItemStack potionColor(int rgb) {
        item.editMeta(meta -> {
            PotionMeta meta1 = (PotionMeta) meta;
            meta1.setColor(org.bukkit.Color.fromRGB(rgb));
        });
        return this;
    }

    public BukkitItemStack potionColor(org.bukkit.Color color) {
        item.editMeta(meta -> {
            PotionMeta meta1 = (PotionMeta) meta;
            meta1.setColor(color);
        });
        return this;
    }

    @Override
    public BukkitItemStack leatherColor(String hexColor) {
        Color color = Color.decode(hexColor);
        item.editMeta(meta -> ((LeatherArmorMeta)meta).setColor(org.bukkit.Color.fromRGB(color.getRed(), color.getGreen(), color.getBlue())));
        return this;
    }

    @Override
    public BukkitItemStack type(Material material) {
        if(customHeadId!=-1 && item!=null && (item.getType()==Material.PLAYER_HEAD || item.getType()==Material.PLAYER_WALL_HEAD) && !(material==Material.PLAYER_HEAD || material==Material.PLAYER_WALL_HEAD)) customHeadId = -1;
        if(item==null){
            item = new ItemStack(material);
        } else {
            item.setType(material);
        }
        if(material!=Material.AIR && !item.hasItemMeta()) item.setItemMeta(Bukkit.getItemFactory().getItemMeta(item.getType()));
        return this;
    }

    @Override
    public BukkitItemStack type(String material) {
        return type(Material.valueOf(material.toUpperCase()));
    }

    @Override
    public BukkitItemStack lore(List<?> lore) {
        List<Component> newLore = new ArrayList<>();
//        for (Object o : lore) {
//            if(o instanceof String s) {
//                newLore.add(PlainTextComponentSerializer.plainText().deserialize(Commons.colorizeWithHex(s)));
//            } else if(o instanceof Component c){
//                newLore.add(c);
//            } else if(o instanceof TextComponent tc){
//                newLore.add(BungeeComponentSerializer.get().deserialize(new BaseComponent[]{tc}));
//            } else if(o instanceof TextComponent[] tc){
//                newLore.add(BungeeComponentSerializer.get().deserialize(tc));
//            } else if(o instanceof BaseComponent[] tc){
//                newLore.add(BungeeComponentSerializer.get().deserialize(tc));
//            }
//        }
        item.editMeta(meta -> meta.lore(newLore));
        return this;
    }

    @Override
    public BukkitItemStack lore(List<String> lore, boolean minimessage) {
        List<net.kyori.adventure.text.Component> newLore = new ArrayList<>();
        lore.forEach(s -> newLore.add(minimessage ? MiniMessage.miniMessage().deserialize(s) : LegacyComponentSerializer.legacySection().deserialize(colorize(s))));
        item.editMeta(itemMeta -> itemMeta.lore(newLore));
        return this;
    }

    @Override
    public BukkitItemStack unbreakable(boolean b) {
        item.editMeta(meta -> meta.setUnbreakable(b));
        return this;
    }

    @Override
    public BukkitItemStack enchant(EnchantmentType e, int level) {
        item.addUnsafeEnchantment(Enchantment.getByKey(NamespacedKey.minecraft(e.namespacedKey())), level);
        return this;
    }

    @Override
    public BukkitItemStack flags(int flags) {
        List<ru.shk.commons.utils.items.universal.ItemFlag> f = ru.shk.commons.utils.items.universal.ItemFlag.fromInt(flags);
        item.editMeta(meta -> f.forEach(itemFlag -> meta.addItemFlags(ItemFlag.valueOf(itemFlag.bukkitName()))));
        return this;
    }

    @Override
    public BukkitItemStack customHeadId(int id) {
        customHeadId = id;
        return this;
    }

    public BukkitItemStack enchant(Enchantment e, int level){
        item.addUnsafeEnchantment(e, level);
        return this;
    }

    public BukkitItemStack bukkitFlags(ItemFlag... flags){
        item.editMeta(meta -> {
            for (ItemFlag flag : flags) meta.addItemFlags(flag);
        });
        return this;
    }
    public BukkitItemStack bukkitFlags(List<ItemFlag> flags){
        item.editMeta(meta -> {
            for (ItemFlag flag : flags) meta.addItemFlags(flag);
        });
        return this;
    }

    @Override
    public BukkitItemStack amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    @Override
    public BukkitItemStack damage(int damage) {
        item.editMeta(meta -> ((Damageable)meta).setDamage(damage));
        return this;
    }

    private BukkitItemStack localHeadOwner(OfflinePlayer p){
        this.customHeadId = -1;
        item.editMeta(meta -> ((SkullMeta)meta).setOwningPlayer(p));
        return this;
    }

    @Override
    public BukkitItemStack localHeadOwner(String name) {
        return localHeadOwner(Bukkit.getOfflinePlayer(name));
    }

    @Override
    public BukkitItemStack localHeadOwner(UUID uuid) {
        return localHeadOwner(Bukkit.getOfflinePlayer(uuid));
    }

    @Override
    public BukkitItemStack base64head(String base64) {
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
    public BukkitItemStack potionData(PotionData potionData) {
        item.editMeta(meta -> {
            PotionMeta m = (PotionMeta) meta;
            m.setBasePotionData(new org.bukkit.potion.PotionData(PotionType.valueOf(potionData.type().name()), potionData.extended(), potionData.upgraded()));
        });
        return this;
    }

    @Override
    public BukkitItemStack customPotion(ru.shk.commons.utils.items.universal.PotionEffect effect) {
        item.editMeta(meta -> {
            PotionMeta m = (PotionMeta) meta;
            m.addCustomEffect(new PotionEffect(PotionEffectType.getByKey(NamespacedKey.minecraft(effect.type().minecraftKey())), effect.duration(), effect.amplifier(), effect.ambient(), effect.particles(), effect.icon()), true);
        });
        return this;
    }

    @Override
    public int customHeadId() {
        return customHeadId;
    }

    @Override
    public Integer potionColor() {
        if(!(item.getItemMeta() instanceof PotionMeta meta) || meta.getColor()==null) return null;
        return meta.getColor().asRGB();
    }

    @Override
    public String displayName() {
        return item.getItemMeta().getDisplayName();
    }

    @Override
    public Integer customModelData() {
        if(!item.getItemMeta().hasCustomModelData()) return null;
        return item.getItemMeta().getCustomModelData();
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
    public Integer damage() {
        if(!(item.getItemMeta() instanceof Damageable d)) return null;
        return d.getDamage();
    }

    @Override
    public String headOwnerName() {
        OfflinePlayer p = ((SkullMeta)item.getItemMeta()).getOwningPlayer();
        return p==null?null:p.getName();
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
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException ignored) {
            return null;
        }
    }

    @Override
    public String potionData() {
        if(item.getItemMeta()==null || !(item.getItemMeta() instanceof PotionMeta meta)) return null;
        org.bukkit.potion.PotionData data = meta.getBasePotionData();
        return new PotionData(PotionData.Type.valueOf(data.getType().name()), data.isExtended(), data.isUpgraded()).toString();
    }

    @Override
    public String customPotion() {
        if(item.getItemMeta()==null || !(item.getItemMeta() instanceof PotionMeta meta)) return null;
        if(meta.getCustomEffects().size()==0) return null;
        PotionEffect effect = meta.getCustomEffects().get(0);
        return new ru.shk.commons.utils.items.universal.PotionEffect(ru.shk.commons.utils.items.universal.PotionType.byKey(effect.getType().getKey().getKey()), effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.hasIcon()).toString();
    }

    @Override
    public BukkitItemStack clone() {
        return new BukkitItemStack(item.clone());
    }

    @Override
    public ItemStack build() {
        return item;
    }

    public static BukkitItemStack fromString(String s){
        return (BukkitItemStack) ItemStackConverter.fromString(s);
    }

    @Override
    public String toString() {
        return ItemStackConverter.toString(this);
    }
}
