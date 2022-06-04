package ru.shk.commons.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.banner.Pattern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.shk.configapi.Config;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ItemStackBuilder {

    private ItemStack stack;
    private ItemMeta meta;

    public ItemStackBuilder(@NonNull Material material) {
        this(new ItemStack(material));
    }

    public ItemStackBuilder(@NonNull ItemStack stack) {
        this.stack = stack;
        meta();
    }

    public ItemStackBuilder(@NonNull ConfigurationSection section){
        Material type = Material.valueOf(section.getString("type").toUpperCase());
        stack = new ItemStack(type);
        meta();
        Config.getIfHasString(section, "display-name", this::displayName);
        Config.getIfHasStringList(section, "lore", this::lore);
        Config.getIfHasStringList(section, "enchant", list -> {
            for (String line : list) {
                String[] parts = line.split(" ");
                Enchantment e = Enchantment.getByName(parts[0].toUpperCase());
                if(e==null){
                    Bukkit.getLogger().warning("Item "+type+" provided with wrong enchantment name: "+parts[0].toUpperCase());
                    continue;
                }
                int level = Integer.parseInt(parts[1]);
                this.enchant(e, level);
            }
        });
        if(type.name().endsWith("POTION")){
            Config.getIfHasStringList(section, "potion-effects", strings -> {
                for (String line : strings) {
                    String[] parts = line.split(" ");
                    var potionType = PotionEffectType.getByName(parts[0].toUpperCase());
                    if(potionType==null){
                        Bukkit.getLogger().warning("Item "+type+" provided with wrong potion effect name: "+parts[0].toUpperCase());
                        continue;
                    }
                    int duration = Integer.parseInt(parts[1]);
                    int amplifer = Integer.parseInt(parts[2]);
                    boolean ambient = parts.length > 3 && Boolean.parseBoolean(parts[3]);
                    boolean particles = parts.length > 4 && Boolean.parseBoolean(parts[4]);
                    boolean icon = parts.length > 5 && Boolean.parseBoolean(parts[5]);
                    PotionEffect effect = new PotionEffect(potionType, duration, amplifer, ambient, particles, icon);
                    addCustomPotionEffect(effect);
                }
            });
        }
        if(type==Material.PLAYER_HEAD) {
            Config.getIfHasString(section, "head-owner", s -> skullOwner(Bukkit.getOfflinePlayer(s)));
            Config.getIfHasString(section, "head-base64", this::base64Head);
        }
        Config.getIfHasBoolean(section, "unbreakable", this::unbreakable);
        Config.getIfHasBoolean(section, "hide-flags", aBoolean -> {
            if(aBoolean) disableFlags();
        });
        Config.getIfHasStringList(section, "flags", strings -> strings.forEach(s -> {
            try {
                ItemFlag flag = ItemFlag.valueOf(s.toUpperCase());
                flags(flag);
            } catch (Exception e){
                Bukkit.getLogger().warning("Wrong ItemFlag provided for item "+type+": "+s+": "+e.getMessage()+". It's being ignored.");
            }
        }));
        if(section.contains("attributes")) section.getMapList("attributes").forEach(map -> {
            try {
                addAttribute((String) map.get("attribute"), (String) map.get("display-name"), (double) map.get("value"), (String) map.get("operation"));
            } catch (IllegalArgumentException e){
                Bukkit.getLogger().warning("Item "+type+" has one or more wrong arguments for 'attributes': "+e.getMessage()+". It's being ignored.");
            }
        });
        Config.getIfHasInt(section, "amount", this::count);
    }

    private ItemStackBuilder addAttribute(String attribute, String displayName, double value, String operation) throws IllegalArgumentException {
        return addAttribute(Attribute.valueOf(attribute), displayName, value, AttributeModifier.Operation.valueOf(operation));
    }
    private ItemStackBuilder addAttribute(Attribute attribute, String displayName, double value, AttributeModifier.Operation operation){
        meta.addAttributeModifier(attribute, new AttributeModifier(displayName, value, operation));
        return this;
    }

    private void meta(){
        meta = stack.getItemMeta();
    }

    public ItemStackBuilder displayName(@NonNull String name) {
        meta.setDisplayName(colorize(name));
        return this;
    }

    public ItemStackBuilder leatherColor(Color color){
        LeatherArmorMeta meta = (LeatherArmorMeta) this.meta;
        meta.setColor(color);
        this.meta = meta;
        return this;
    }

    public ItemStackBuilder type(@NonNull Material material) {
        stack.setType(material);
        return this;
    }

    public ItemStackBuilder lore(@NonNull String... lore) {
        meta.setLore(Stream.of(lore).map(this::colorize)
                .collect(Collectors.toList()));
        return this;
    }
    public ItemStackBuilder lore(@NonNull List<String> lore) {
        meta.setLore(lore.stream().map(this::colorize)
                .collect(Collectors.toList()));
        return this;
    }

    public ItemStackBuilder unbreakable(boolean is) {
        meta.setUnbreakable(is);
        return this;
    }

    public ItemStackBuilder enchant(@Nullable Enchantment enchantment, int level) {
        if(enchantment==null) return this;
        if (stack.getType() == Material.ENCHANTED_BOOK) {
            EnchantmentStorageMeta tempMeta = (EnchantmentStorageMeta) meta;
            tempMeta.addStoredEnchant(enchantment, level, true);
            meta = tempMeta;
            return this;
        }

        meta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemStackBuilder enchants(@NonNull Enchantment ...enchantments) {
        for (final Enchantment enchantment : enchantments) {
            enchant(enchantment, enchantment.getMaxLevel());
        }
        return this;
    }

    public ItemStackBuilder disableFlags() {
        return flags(ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_PLACED_ON,
                ItemFlag.HIDE_POTION_EFFECTS);
    }

    public ItemStackBuilder flags(@NonNull ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    @Deprecated
    public ItemStackBuilder durability(int durability) {
        stack.setDurability((short) durability);
        return this;
    }

    public ItemStackBuilder count(int count) {
        stack.setAmount(count);
        return this;
    }

    public ItemStackBuilder potionData(@NonNull PotionData data) {
        if (isPotion()) {
            PotionMeta tempMeta = (PotionMeta) meta;
            tempMeta.setBasePotionData(data);
            meta = tempMeta;
        }
        return this;
    }

    public ItemStackBuilder addCustomPotionEffect(@NonNull PotionEffect effect) {
        if (isPotion()) {
            PotionMeta tempMeta = (PotionMeta) meta;
            tempMeta.addCustomEffect(effect, true);
            meta = tempMeta;
        }
        return this;
    }

    public ItemStackBuilder skullOwner(@NonNull OfflinePlayer player) {
        if (stack.getType() == Material.PLAYER_HEAD) {
            SkullMeta tempMeta = (SkullMeta) meta;
            tempMeta.setOwningPlayer(player);
            meta = tempMeta;
        }
        return this;
    }
    public ItemStackBuilder base64Head(String texture) {
        SkullMeta skullMeta = (SkullMeta) meta;
        GameProfile profile = new GameProfile(UUID.randomUUID(), "");
        profile.getProperties().put("textures", new Property("textures", texture));
        Field profileField;
        try {
            profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, profile);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
        }
        this.meta = skullMeta;
        return this;
    }

    public ItemStackBuilder bannerPattern(@NonNull Pattern pattern) {
        if (isBanner()) {
            BannerMeta tempMeta = (BannerMeta) meta;
            tempMeta.addPattern(pattern);
            meta = tempMeta;
        }
        return this;
    }

    public ItemStackBuilder supplyStack(@NonNull UnaryOperator<ItemStack> stack) {
        this.stack = stack.apply(this.stack);
        return this;
    }

    public ItemStackBuilder supplyMeta(@NonNull UnaryOperator<ItemMeta> meta) {
        this.meta = meta.apply(this.meta);
        return this;
    }

    public ItemStack build() {
        stack.setItemMeta(meta);
        return stack;
    }

    private boolean isPotion() {
        return stack.getType() == Material.POTION || stack.getType() == Material.SPLASH_POTION || stack.getType() == Material.LINGERING_POTION;
    }

    private boolean isBanner() {
        return stack.getType() == Material.BLACK_BANNER ||
                stack.getType() == Material.BLUE_BANNER ||
                stack.getType() == Material.BROWN_BANNER ||
                stack.getType() == Material.CYAN_BANNER ||
                stack.getType() == Material.GREEN_BANNER ||
                stack.getType() == Material.GRAY_BANNER ||
                stack.getType() == Material.LIGHT_GRAY_BANNER ||
                stack.getType() == Material.MAGENTA_BANNER ||
                stack.getType() == Material.ORANGE_BANNER ||
                stack.getType() == Material.YELLOW_BANNER ||
                stack.getType() == Material.WHITE_BANNER ||
                stack.getType() == Material.LIME_BANNER ||
                stack.getType() == Material.PINK_BANNER;
    }

    private String colorize(String s){
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @Override
    public ItemStackBuilder clone(){
        return new ItemStackBuilder(build());
    }
}
