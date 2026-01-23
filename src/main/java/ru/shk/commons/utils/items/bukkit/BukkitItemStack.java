package ru.shk.commons.utils.items.bukkit;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class BukkitItemStack extends ItemStackBuilder<ItemStack, Material, BukkitItemStack> {
    private int customHeadId = -1;
    private ItemStack item = new ItemStack(Material.AIR);

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
    public BukkitItemStack enchantingGlint(boolean value) {
        item.editMeta(itemMeta -> itemMeta.setEnchantmentGlintOverride(value));
        return this;
    }

    @Override
    public BukkitItemStack customHead(int id) {
        String texture = Commons.getInstance().getCustomHeadTexture(id);
        if (texture==null) return this;
        base64head(texture);
        this.customHeadId = id;
        return this;
    }

    @Override
    public BukkitItemStack customHead(String key) {
        CustomHead h = Commons.getInstance().findCustomHead(key);
        if(h==null) return this;
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

    @Override@Deprecated
    public BukkitItemStack customModelData(int id) {
        item.editMeta(meta -> meta.setCustomModelData(id));
        return this;
    }

    @Override
    public BukkitItemStack customModelData(String id) {
        item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString(id).build());
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
        if(lore.isEmpty()) {
            item.lore(Collections.EMPTY_LIST);
            return this;
        }
        if(lore.get(0) instanceof String) return lore((List<String>) lore, false);
        List<Component> newLore = new ArrayList<>();
        for (Object o : lore) {
            if(o instanceof String s) {
                newLore.add(PlainTextComponentSerializer.plainText().deserialize(Commons.colorizeWithHex(s)));
            } else if(o instanceof Component c){
                newLore.add(c);
            }
        }
        item.editMeta(meta -> meta.lore(newLore));
        return this;
    }

    @Override
    public BukkitItemStack lore(List<String> lore, boolean minimessage) {
        return lore(lore, minimessage, true);
    }


    public BukkitItemStack lore(List<String> lore, boolean minimessage, boolean forceDisableItalic) {
        List<net.kyori.adventure.text.Component> newLore = new ArrayList<>();
        lore.forEach(s -> {
            Component c;
            if(s.isEmpty()) {
                c = Component.text(" ");
            } else {
                c = minimessage ? MiniMessage.miniMessage().deserialize(s) : LegacyComponentSerializer.legacySection().deserialize(colorize(s));
            }
            if(forceDisableItalic) c = c.decoration(TextDecoration.ITALIC, false);
            newLore.add(c);
        });
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

    public BukkitItemStack flags(int flags) {
        List<ru.shk.commons.utils.items.universal.ItemFlag> f = ru.shk.commons.utils.items.universal.ItemFlag.fromInt(flags);
        return flags(f);
    }

    @Override
    public BukkitItemStack flags(List<ru.shk.commons.utils.items.universal.ItemFlag> flags) {
        item.editMeta(meta -> flags.forEach(itemFlag -> meta.addItemFlags(ItemFlag.valueOf(itemFlag.bukkitName()))));
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
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "aboba");
        profile.setProperty(new ProfileProperty("textures", base64));
        this.customHeadId = -1;
        item.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile(profile));
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
    public BukkitItemStack maxStackSize(int size) {
        item.setData(DataComponentTypes.MAX_STACK_SIZE, size);
        return this;
    }

    @Override
    public BukkitItemStack itemModel(Key key) {
        item.editMeta(itemMeta -> itemMeta.setItemModel(new NamespacedKey(key.namespace(), key.value())));
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
            return skullMeta.getPlayerProfile().getProperties().stream().filter(profileProperty -> profileProperty.getName().equals("textures")).map(ProfileProperty::getValue).findAny().orElse(null);
        } catch (Throwable t){
            return null;
        }
//        skullMeta.setPlayerProfile(new CraftPlayerProfile(profile));
//        try {
//            Field profileField = skullMeta.getClass().getDeclaredField("profile");
//            profileField.setAccessible(true);
//            GameProfile profile = (GameProfile) profileField.get(skullMeta);
//            Collection<Property> collection = profile.getProperties().get("textures");
//            return collection.stream().filter(property -> property.name().equals("textures")).findAny().get().value();
//        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException ignored) {
//            return null;
//        }
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
    public Integer maxStackSize() {
        return item.getData(DataComponentTypes.MAX_STACK_SIZE).intValue();
    }

    @Override
    public Key itemModel() {
        return item.getItemMeta().getItemModel()==null?null:item.getItemMeta().getItemModel().key();
    }

    public BukkitItemStack editMeta(Consumer<ItemMeta> consumer){
        item.editMeta(consumer);
        return this;
    }

    public <T extends ItemMeta> BukkitItemStack editMeta(Class<T> clazz, Consumer<T> consumer){
        item.editMeta(clazz, consumer);
        return this;
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
