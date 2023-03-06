package ru.shk.commons.utils.items.protocolize;

import dev.simplix.protocolize.api.item.ItemStack;
import dev.simplix.protocolize.data.ItemType;
import land.shield.playerapi.CachedPlayer;
import lombok.NonNull;
import net.querz.nbt.tag.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.shk.commons.ServerType;
import ru.shk.commons.utils.items.ItemStackBuilder;
import ru.shk.commons.utils.items.ItemStackConverter;
import ru.shk.commons.utils.items.bungee.BungeeItemStack;
import ru.shk.commons.utils.items.universal.*;
import ru.shk.commons.utils.items.velocity.VelocityItemStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class ProtocolizeItemStack<R extends ProtocolizeItemStack> extends ItemStackBuilder<ItemStack, ItemType, R> {
    private int customHeadId = -1;
    private ItemStack item;

    public ProtocolizeItemStack() {
        item = new ItemStack(ItemType.AIR);
    }

    public ProtocolizeItemStack(@NonNull ItemStack stack) {
        this.item = stack;
    }

    public ProtocolizeItemStack(@NonNull ItemType item) {
        this.item = new ItemStack(item);
    }

    public ProtocolizeItemStack(@NonNull String type) {
        type(type);
    }

    @Override
    public R displayName(String name) {
        item.displayName(stringToComponent(colorize(name)));
        return (R) this;
    }

    @Override
    public R displayName(Object name) {
        item.displayName(name);
        return (R) this;
    }

    @Override
    public R customModelData(int id) {
        item.nbtData().put("CustomModelData", new IntTag(id));
        return (R) this;
    }

    @Override
    public R leatherColor(Color color) {
        CompoundTag tag = item.nbtData().getCompoundTag("display");
        tag.put("color", new IntTag(color.getRGB()));
        return (R) this;
    }

    @Override
    public R leatherColor(String hexColor) {
        Color color = Color.decode(hexColor);
        CompoundTag tag = item.nbtData().getCompoundTag("display");
        tag.put("color", new IntTag(color.getRGB()));
        return (R) this;
    }

    @Override
    public R type(ItemType type) {
        item.itemType(type);
        return (R) this;
    }

    @Override
    public R type(String material) {
        item.itemType(ItemType.valueOf(material.toUpperCase()));
        return (R) this;
    }

    @Override
    public R lore(java.util.List<String> lore) {
        item.lore(stringsToComponentList(lore.stream().map(this::colorize).toList()), false);
        return (R) this;
    }

    @Override
    public R unbreakable(boolean b) {
        item.nbtData().put("Unbreakable", new ByteTag((byte)(b?1:0)));
        return (R) this;
    }

    @Override
    public R enchant(EnchantmentType e, int level) {
        ListTag<?> array = item.nbtData().getListTag("Enchantments");
        if(array!=null) {
            for (int i = 0; i < array.size(); i++) {
                CompoundTag enchantment = (CompoundTag) array.get(i);
                String id = enchantment.getString("id");
                if(!id.equals("minecraft:"+e.namespacedKey())) continue;
                int l = enchantment.getInt("lvl");
                if(l!=level) enchantment.put("lvl", new IntTag(level));
                return (R) this;
            }
        }
        ListTag<CompoundTag> newList = new ListTag<>(CompoundTag.class);
        if(array!=null) array.forEach(tag -> newList.add((CompoundTag) tag));
        CompoundTag eTag = new CompoundTag();
        eTag.put("id", new StringTag("minecraft:"+e.namespacedKey()));
        eTag.put("lvl", new ShortTag((short) level));
        newList.add(eTag);
        item.nbtData().put("Enchantments", newList);
        return (R) this;
    }

    @Override
    public R flags(int flags) {
        item.nbtData().put("HideFlags", new IntTag(flags));
        return (R) this;
    }

    @Override
    public R amount(int amount) {
        item.amount((byte) amount);
        return (R) this;
    }

    @Override
    public R damage(int damage) {
        item.nbtData().put("Damage", new IntTag(damage));
        return (R) this;
    }

    @Override
    public R potionColor(int rgb) {
        item.nbtData().put("CustomPotionColor", new IntTag(rgb));
        return (R) this;
    }

    @Override
    public Integer potionColor() {
        if(!item.nbtData().containsKey("CustomPotionColor")) return null;
        return item.nbtData().getInt("CustomPotionColor");
    }

    @Override
    public R headOwner(String name) {
        String texture = ItemStackBuilder.headsCache().getPlayerTexture(name);
        if(texture==null) {
            customHeadId = -1;
            clearHeadOwnerTag();
            item.nbtData().put("SkullOwner", new StringTag(name));
            return (R) this;
        }
        base64head(texture);
        return (R) this;
    }

    private void clearHeadOwnerTag(){
        item.nbtData().remove("SkullOwner");
    }

    @Override
    public R headOwner(UUID uuid) {
        String texture = ItemStackBuilder.headsCache().getPlayerTexture(uuid);
        if(texture==null) {
            customHeadId = -1;
            clearHeadOwnerTag();
            return (R) this;
        }
        base64head(texture);
        return (R) this;
    }

    @Override
    public R headOwner(CachedPlayer player) {
        String texture = ItemStackBuilder.headsCache().getPlayerTexture(player);
        if(texture==null) {
            customHeadId = -1;
            clearHeadOwnerTag();
            item.nbtData().put("SkullOwner", new StringTag(player.getName()));
            return (R) this;
        }
        base64head(texture);
        return (R) this;
    }

    @Override
    public R base64head(String base64) {
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

        textureTag.put("Value", new StringTag(base64));
        texturesTag.add(textureTag);
        propertiesTag.put("textures", texturesTag);
        skullOwnerTag.put("Properties", propertiesTag);
        skullOwnerTag.put("Name", new StringTag("##aboba"));

        tag.put("SkullOwner", skullOwnerTag);

        tag.put("HideFlags", new IntTag(99));
        tag.put("overrideMeta", new ByteTag((byte)1));
        item.nbtData(tag);
        return (R) this;
    }

    @Override
    public R potionData(PotionData potionData) {
        String potion = (potionData.extended()?"long_":"")+(potionData.upgraded()?"strong_":"")+potionData.type().name().toLowerCase();
        item.nbtData().put("Potion", new StringTag(potion));
        return (R) this;
    }

    @Override
    public R customPotion(PotionEffect potionEffect) {
        final @NotNull ListTag<@NotNull CompoundTag> effects = new ListTag<>(CompoundTag.class);
        CompoundTag tag = new CompoundTag();
        tag.put("id", new IntTag(potionEffect.type().id()));
        tag.put("Duration", new IntTag(potionEffect.duration()));
        tag.put("Amplifier", new IntTag(potionEffect.amplifier()));
        tag.put("Ambient", new ByteTag((byte) (potionEffect.ambient()?1:0)));
        tag.put("ShowParticles", new ByteTag((byte) (potionEffect.particles()?1:0)));
        tag.put("ShowIcon", new ByteTag((byte) (potionEffect.icon()?1:0)));
        effects.add(tag);
        item.nbtData().put("CustomPotionEffects", effects);
        return (R) this;
    }

    @Override
    public int customHeadId() {
        return customHeadId;
    }

    @Override
    public R customHeadId(int id) {
        this.customHeadId = id;
        return (R) this;
    }

    @Override
    public String displayName() {
        return item.displayName(true);
    }

    @Override
    public Integer customModelData() {
        return item.nbtData().containsKey("CustomModelData")?item.nbtData().getInt("CustomModelData"):null;
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
    public java.util.List<String> lore() {
        return item.lore(true);
    }

    @Override
    public boolean isUnbreakable() {
        return item.nbtData().containsKey("Unbreakable") && item.nbtData().getByte("Unbreakable")==1;
    }

    @Override
    public java.util.List<Enchantment> enchantments() {
        java.util.List<Enchantment> list = new ArrayList<>();
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
    public java.util.List<ItemFlag> flags() {
        if(!item.nbtData().containsKey("HideFlags")) return new ArrayList<>();
        return ItemFlag.fromInt(item.nbtData().getInt("HideFlags"));
    }

    @Override
    public int amount() {
        return item.amount();
    }

    @Override
    public Integer damage() {
        return item.nbtData().containsKey("Damage")?item.nbtData().getInt("Damage"):null;
    }

    @Override
    public String headOwnerName() {
        @Nullable CompoundTag skullOwnerTag = item.nbtData().getCompoundTag("SkullOwner");
        if(skullOwnerTag==null) return null;
        return skullOwnerTag.containsKey("Name")?skullOwnerTag.getString("Name"):null;
    }

    @Override
    public String base64head() {
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

    @Override
    public String potionData() {
        if(!item.nbtData().containsKey("Potion")) return null;
        String potionNbt = item.nbtData().getString("Potion");
        String type = potionNbt.replace("long_","").replace("strong_", "").toUpperCase();
        boolean extended = potionNbt.startsWith("long_");
        boolean upgraded = potionNbt.startsWith("strong_");
        return new PotionData(PotionData.Type.valueOf(type), extended, upgraded).toString();
    }

    @Override
    public String customPotion() {
        if(!item.nbtData().containsKey("CustomPotionEffects")) return null;
        ListTag<CompoundTag> list = (ListTag<CompoundTag>) item.nbtData().getListTag("CustomPotionEffects");
        if(list.size()==0) return null;
        CompoundTag tag = list.get(0);
        int id = tag.getInt("id");
        int duration = tag.getInt("Duration");
        int amplifier = tag.getInt("Amplifier");
        boolean ambient = tag.getBoolean("Ambient");
        boolean particles = tag.getBoolean("ShowParticles");;
        boolean icon = tag.getBoolean("ShowIcon");
        return new PotionEffect(PotionType.byId(id), duration, amplifier, ambient, particles, icon).toString();
    }

    @Override
    public R clone() {
        return (R) switch (ServerType.get()){
            case BUNGEE -> new BungeeItemStack(item.deepClone());
            case VELOCITY -> new VelocityItemStack(item.deepClone());
            default -> throw new IllegalStateException("This class cannot be used on "+ServerType.get()+" server.");
        };
    }

    @Override
    public ItemStack build() {
        return item;
    }

    public static BungeeItemStack fromString(String s){
        return (BungeeItemStack) ItemStackConverter.fromString(s);
    }

    @Override
    public String toString() {
        return ItemStackConverter.toString(this);
    }

    public abstract Object stringToComponent(String s);

    public List<Object> stringsToComponentList(List<String> s){
        return s.stream().map(this::stringToComponent).toList();
    }

}
