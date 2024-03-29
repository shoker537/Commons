package ru.shk.commons.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.banner.Pattern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.shk.commons.Commons;
import ru.shk.configapi.Config;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Deprecated
public class ItemStackBuilder {

    private ItemStack stack;
    private ItemMeta meta;
    @Getter private int customHeadId = -1;

    public ItemStackBuilder(){
        this(new ItemStack(Material.AIR));
    }
    public ItemStackBuilder(@NonNull Material material) {
        this(new ItemStack(material));
    }

    public ItemStackBuilder(@NonNull ItemStack stack) {
        this.stack = stack;
        meta();
    }

    @Override
    public String toString() {
        return ItemStackConverter.toString(this);
    }

    public ItemStackBuilder customHead(int id){
        base64Head(Commons.getInstance().getCustomHeadTexture(id));
        this.customHeadId = id;
        return this;
    }

    public ItemStackBuilder customHead(String key){
        CustomHead h = Commons.getInstance().findCustomHead(key);
        if(h==null) return null;
        base64Head(h.getTexture());
        this.customHeadId = h.getId();
        return this;
    }

    public ItemStackBuilder(@NonNull ConfigurationSection section){
        Material type = Material.valueOf(section.getString("type").toUpperCase());
        if(type==Material.PLAYER_HEAD){
            if(section.contains("custom-head-id")){
                Object id = section.get("custom-head-id");
                if(id instanceof Integer i){
                    stack = Commons.getInstance().getCustomHead(i).build();
                } else if(id instanceof String s) {
                    stack = Commons.getInstance().getCustomHead(s).build();
                } else {
                    stack = new ItemStack(type);
                    Commons.getInstance().warning("Item has an invalid custom-head-id value: "+id.getClass().getSimpleName()+". Only int/string supported.");
                }
            } else {
                stack = new ItemStack(type);
            }
        } else {
            stack = new ItemStack(type);
        }
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
                String name = (String) map.get("attribute");
                String displayName = (String) map.get("display-name");
                double value = (double) map.get("value");
                String operation = (String) map.get("operation");
                String slot = map.containsKey("slot")?(String) map.get("slot"):null;
                UUID uuid = map.containsKey("uuid")?UUID.fromString((String) map.get("uuid")):new UUID(1,1);

                autoAddAttribute(uuid, Attribute.valueOf(name.toUpperCase()), displayName, value, AttributeModifier.Operation.valueOf(operation), slot);
            } catch (IllegalArgumentException e){
                Bukkit.getLogger().warning("Item "+type+" has one or more wrong arguments for 'attributes': "+e.getMessage()+". It's being ignored.");
            }
        });
        Config.getIfHasInt(section, "amount", this::count);
        Config.getIfHasInt(section, "custom-model-data", this::customModelData);
    }

    private ItemStackBuilder autoAddAttribute(UUID attributeUUID, Attribute attribute, String displayName, double value, AttributeModifier.Operation operation, String slot) {
        if(slot!=null) return addAttribute(attributeUUID, attribute, displayName, value, operation, EquipmentSlot.valueOf(slot.toUpperCase()));
        return addAttribute(attribute, displayName, value, operation);
    }
    private ItemStackBuilder addAttribute(Attribute attribute, String displayName, double value, AttributeModifier.Operation operation){
        meta.addAttributeModifier(attribute, new AttributeModifier(displayName, value, operation));
        return this;
    }
    private ItemStackBuilder addAttribute(UUID attributeUUID, Attribute attribute, String displayName, double value, AttributeModifier.Operation operation, EquipmentSlot slot){
        AttributeModifier am = new AttributeModifier(attributeUUID, displayName, value, operation, slot);
        meta.addAttributeModifier(attribute, am);
        return this;
    }

    private void meta(){
        meta = stack.getItemMeta();
        if(meta==null) meta = Bukkit.getItemFactory().getItemMeta(stack.getType());
    }

    public ItemStackBuilder displayName(@NonNull String name) {
        meta.setDisplayName(colorize(name));
        return this;
    }

    public ItemStackBuilder customModelData(int number){
        meta.setCustomModelData(number);
        return this;
    }

    public int customModelData(){
        return meta.getCustomModelData();
    }

    public ItemStackBuilder leatherColor(Color color){
        LeatherArmorMeta meta = (LeatherArmorMeta) this.meta;
        meta.setColor(color);
        this.meta = meta;
        return this;
    }
    public ItemStackBuilder leatherColor(java.awt.Color color){
        LeatherArmorMeta meta = (LeatherArmorMeta) this.meta;
        meta.setColor(Color.fromRGB(color.getRed(), color.getGreen(), color.getBlue()));
        this.meta = meta;
        return this;
    }

    public String leatherColor(){
        LeatherArmorMeta meta = (LeatherArmorMeta) this.meta;
        Color c = meta.getColor();
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    public ItemStackBuilder type(@NonNull Material material) {
        stack.setType(material);
        return this;
    }

    public Material type(){
        return stack.getType();
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

    public int count(){
        return stack.getAmount();
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
            this.customHeadId = -1;
            meta = tempMeta;
        }
        return this;
    }
    public ItemStackBuilder skullOwner(@NonNull UUID player) {
        if (stack.getType() == Material.PLAYER_HEAD) {
            SkullMeta tempMeta = (SkullMeta) meta;
            tempMeta.setOwningPlayer(Bukkit.getOfflinePlayer(player));
            this.customHeadId = -1;
            meta = tempMeta;
        }
        return this;
    }
    public ItemStackBuilder skullOwner(@NonNull String player) {
        if (stack.getType() == Material.PLAYER_HEAD) {
            SkullMeta tempMeta = (SkullMeta) meta;
            tempMeta.setOwningPlayer(Bukkit.getOfflinePlayer(player));
            this.customHeadId = -1;
            meta = tempMeta;
        }
        return this;
    }
    public String skullOwner(){
        return ((SkullMeta)stack.getItemMeta()).getOwningPlayer().getName();
    }
    public UUID skullOwnerUUID(){
        return ((SkullMeta)stack.getItemMeta()).getOwningPlayer().getUniqueId();
    }
    public ItemStackBuilder base64Head(String texture) {
        SkullMeta skullMeta = (SkullMeta) meta;
        GameProfile profile = new GameProfile(new UUID(0,0), "");
        profile.getProperties().put("textures", new Property("textures", texture));
        Field profileField;
        try {
            profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, profile);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
        }
        this.customHeadId = -1;
        this.meta = skullMeta;
        return this;
    }

    public String base64Head() {
        SkullMeta skullMeta = (SkullMeta) meta;
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
        return Commons.colorizeWithHex(s);
    }

    @Override
    public ItemStackBuilder clone(){
        return new ItemStackBuilder(build());
    }
}
