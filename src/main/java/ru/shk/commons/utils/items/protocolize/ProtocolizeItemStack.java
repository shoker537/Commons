package ru.shk.commons.utils.items.protocolize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.simplix.protocolize.api.chat.ChatElement;
import dev.simplix.protocolize.api.item.BaseItemStack;
import dev.simplix.protocolize.api.item.ItemStack;
import dev.simplix.protocolize.api.item.MobEffectInstance;
import dev.simplix.protocolize.api.item.component.*;
import dev.simplix.protocolize.api.util.Property;
import dev.simplix.protocolize.data.ItemType;
import dev.simplix.protocolize.data.MobEffect;
import dev.simplix.protocolize.data.Potion;
import dev.simplix.protocolize.data.item.component.*;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ru.shk.commons.ServerType;
import ru.shk.commons.utils.Logger;
import ru.shk.commons.utils.items.ItemStackBuilder;
import ru.shk.commons.utils.items.ItemStackConverter;
import ru.shk.commons.utils.items.bungee.BungeeItemStack;
import ru.shk.commons.utils.items.universal.*;
import ru.shk.commons.utils.items.velocity.VelocityItemStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public abstract class ProtocolizeItemStack<R extends ProtocolizeItemStack> extends ItemStackBuilder<ItemStack, ItemType, R> {
    private static final Gson gson = new GsonBuilder().create();
    private int customHeadId = -1;
    private final ItemStack item;

    public ProtocolizeItemStack() {
        item = new ItemStack(ItemType.AIR);
    }

    public ProtocolizeItemStack(@NonNull BaseItemStack stack) {
        this.item = (ItemStack) stack.deepClone();
    }

    public ProtocolizeItemStack(@NonNull ItemType item) {
        this.item = new ItemStack(item);
    }

    public ProtocolizeItemStack(@NonNull String type) {
        this.item = new ItemStack(ItemType.valueOf(type.toUpperCase()));
    }

    @Override
    public R enchantingGlint(boolean value) {
        item.addComponent(EnchantmentGlintComponent.create(value));
        return (R) this;
    }

    @Override
    public R displayName(String name) {
        item.displayName(ChatElement.of(stringToComponent(name)));
        return (R) this;
    }

    @Override
    public R displayName(Object name) {
        item.displayName(elementFromObject(name));
        return (R) this;
    }

    public ChatElement<?> elementFromObject(Object o){
        if(o instanceof String s){
            try {
                gson.fromJson(s, JsonObject.class);
                return ChatElement.ofJson(s);
            } catch (JsonSyntaxException t){
                return ChatElement.of(stringToComponent(s));
            }
        } else if(o instanceof Component c) {
            return ChatElement.of(c);
        } else {
            Logger.warning("Unknown object provided for ChatElement: "+o.getClass().getName());
            return ChatElement.ofLegacyText("?");
        }
    }

    @Override
    public R customModelData(int id) {
        CustomModelDataComponent c = item.getComponent(CustomModelDataComponent.class);
        if(c==null) c = CustomModelDataComponent.create(id); else c.setCustomModelData(id);
        item.addComponent(c);
        return (R) this;
    }

    @Override
    public R leatherColor(Color color) {
        //todo: leather color component
//        CompoundTag tag = item.nbtData().getCompoundTag("display");
//        tag.put("color", new IntTag(color.getRGB()));
        return (R) this;
    }

    @Override
    public R leatherColor(String hexColor) {
        //todo: leather color component
//        Color color = Color.decode(hexColor);
//        CompoundTag tag = item.nbtData().getCompoundTag("display");
//        tag.put("color", new IntTag(color.getRGB()));
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
    public R lore(java.util.List<?> lore) {
        if(lore.isEmpty()) {
            item.lore(Collections.EMPTY_LIST);
            return (R) this;
        }
        if(lore.get(0) instanceof String) return lore((List<String>) lore, false);
        List<ChatElement<?>> newLore = new ArrayList<>();
        for (Object o : lore) {
            newLore.add(elementFromObject(o));
        }
        item.lore(newLore);
        return (R) this;
    }

    @Override
    public R lore(List<String> lore, boolean minimessage, boolean forceDisableItalic) {
        List<net.kyori.adventure.text.Component> newLore = new ArrayList<>();
        lore.forEach(s -> {
            Component c;
            if(s.isEmpty()) {
                c = Component.text(" ");
            } else {
                c = minimessage ? MiniMessage.miniMessage().deserialize(s) : LegacyComponentSerializer.legacyAmpersand().deserialize(colorize(s));
            }
            if(forceDisableItalic) c = c.decoration(TextDecoration.ITALIC, false);
            newLore.add(c);
        });
        List<ChatElement<?>> list = new ArrayList<>();
        newLore.forEach(component -> list.add(elementFromObject(component)));
        item.lore(list);
        return (R) this;
    }

    @Override
    public R unbreakable(boolean b) {
        if(b) item.addComponent(UnbreakableComponent.create(true)); else item.removeComponent(UnbreakableComponentImpl.Type.INSTANCE);
        return (R) this;
    }

    @Override
    public R enchant(EnchantmentType e, int level) {
        // ! Proxy does not know enchantments anymore
//        EnchantmentsComponent c = item.getComponent(EnchantmentsComponent.class);
//        if(c==null) c = EnchantmentsComponent.create(new HashMap<>());
//        c.addEnchantment(dev.simplix.protocolize.data.Enchantment.TICK,1);
//        c.addEnchantment(dev.simplix.protocolize.data.Enchantment.valueOf(e.namespacedKey().toUpperCase()), level);
//        item.addComponent(c);
        enchantingGlint(true);
        return (R) this;
    }

    @Override
    public R amount(int amount) {
        item.amount((byte) amount);
        return (R) this;
    }

    @Override
    public R damage(int damage) {
        item.addComponent(DamageComponent.create(damage));
        return (R) this;
    }

    @Override
    public R potionColor(int rgb) {
        PotionContentsComponent c = item.getComponent(PotionContentsComponent.class);
        if(c==null) c = PotionContentsComponent.create(Potion.MUNDANE);
        c.setCustomColor(rgb);
        item.addComponent(c);
        return (R) this;
    }

    @Override
    public Integer potionColor() {
        PotionContentsComponent c = item.getComponent(PotionContentsComponent.class);
        if(c==null) return null;
        return c.getCustomColor();
    }

    @Override
    public R localHeadOwner(String name) {
        customHeadId = -1;
        item.addComponent(ProfileComponent.create(name, null, Collections.EMPTY_LIST));
        return (R) this;
    }

    public void clearComponentTag(StructuredComponentType<?> type){
        item.removeComponent(type);
    }

    public void addComponent(StructuredComponent component){
        item.addComponent(component);
    }

    @Override
    public R localHeadOwner(UUID uuid) {
        customHeadId = -1;
        item.addComponent(ProfileComponent.create("#", uuid, Collections.EMPTY_LIST));
        return (R) this;
    }

    @Override
    public R base64head(String base64) {
        customHeadId = -1;
        item.addComponent(ProfileComponent.create("#", new UUID(0,0), List.of(new Property("textures", base64, null))));
        return (R) this;
    }

    @Override
    public R potionData(PotionData potionData) {
        String potion = "";
        if(potionData.extended()) potion+="LONG_"; else if (potionData.upgraded()) potion+="STRONG_";
        potion+=potionData.type().name().toUpperCase();
        Potion p = Potion.valueOf(potion);
        PotionContentsComponent c = item.getComponent(PotionContentsComponent.class);
        if(c==null) c = PotionContentsComponent.create(p); else c.setPotion(p);
        item.addComponent(c);
        return (R) this;
    }

    @Override
    public R customPotion(PotionEffect potionEffect) {
        PotionContentsComponent c = item.getComponent(PotionContentsComponent.class);
        if(c==null) c = PotionContentsComponent.create(Potion.MUNDANE);
        c.getCustomEffects().clear();
        c.addCustomEffect(new MobEffectInstance(MobEffect.valueOf(potionEffect.type().minecraftKey().toUpperCase()), new MobEffectInstance.Details(potionEffect.amplifier(), potionEffect.duration(), potionEffect.ambient(), potionEffect.particles(), potionEffect.icon(), null)));
        item.addComponent(c);
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
        return item.displayName().asLegacyText();
    }

    @Override
    public Integer customModelData() {
        CustomModelDataComponent c = item.getComponent(CustomModelDataComponent.class);
        if(c==null) return null;
        return c.getCustomModelData();
    }

    @Override
    public R flags(List<ItemFlag> flags) {
        for (StructuredComponent c : item.getComponents()) {
            if(c instanceof EnchantmentsComponentImpl e && flags.contains(ItemFlag.HIDE_ENCHANTMENTS)) e.setShowInTooltip(false);
            else if(c instanceof AttributeModifiersComponentImpl e && flags.contains(ItemFlag.HIDE_MODIFIERS)) e.setShowInTooltip(false);
            else if(c instanceof UnbreakableComponentImpl e && flags.contains(ItemFlag.HIDE_UNBREAKABLE)) e.setShowInTooltip(false);
            //todo: add destroys
            //todo: add placed on
            else if(c instanceof DyedColorComponentImpl e && flags.contains(ItemFlag.HIDE_DYE)) e.setShowInTooltip(false);
        }
        if(flags.contains(ItemFlag.HIDE_ADDITIONAL)) item.addComponent(new HideAdditionalTooltipComponentImpl());
        return (R) this;
    }

    @Override
    public Color leatherColor() {
        //todo leather armor component
//        if(!item.nbtData().containsKey("display") || !item.nbtData().getCompoundTag("display").containsKey("color")) return null;
//        int color = item.nbtData().getCompoundTag("display").getInt("color");
//        return new Color(color);
        return null;
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
        return item.lore().stream().map(ChatElement::asLegacyText).toList();
    }

    @Override
    public boolean isUnbreakable() {
        UnbreakableComponent c = item.getComponent(UnbreakableComponent.class);
        return c != null;
    }

    @Override
    public java.util.List<Enchantment> enchantments() {
        EnchantmentsComponent c = item.getComponent(EnchantmentsComponent.class);
        if(c==null) return Collections.EMPTY_LIST;
        java.util.List<Enchantment> list = new ArrayList<>();
        c.getEnchantments().forEach((enchantment, integer) -> list.add(new Enchantment(EnchantmentType.fromString(enchantment.name()), integer)));
        return list;
    }

    @Override
    public java.util.List<ItemFlag> flags() {
        List<ItemFlag> hidden = new ArrayList<>();
        for (StructuredComponent c : item.getComponents()) {
            if(c instanceof EnchantmentsComponentImpl e && !e.isShowInTooltip()) hidden.add(ItemFlag.HIDE_ENCHANTMENTS);
            else if(c instanceof AttributeModifiersComponentImpl e && !e.isShowInTooltip()) hidden.add(ItemFlag.HIDE_MODIFIERS);
            else if(c instanceof UnbreakableComponentImpl e && !e.isShowInTooltip()) hidden.add(ItemFlag.HIDE_UNBREAKABLE);
            //todo: add destroys
            //todo: add placed on
            else if(c instanceof HideAdditionalTooltipComponentImpl) hidden.add(ItemFlag.HIDE_ADDITIONAL);
            else if(c instanceof DyedColorComponentImpl e && !e.isShowInTooltip()) hidden.add(ItemFlag.HIDE_DYE);
        }
        return hidden;
    }

    @Override
    public int amount() {
        return item.amount();
    }

    @Override
    public Integer damage() {
        DamageComponent damage = item.getComponent(DamageComponentImpl.class);
        if(damage==null) return null;
        return damage.getDamage();
    }

    @Override
    public String headOwnerName() {
        ProfileComponent component = item.getComponent(ProfileComponent.class);
        if(component==null) return null;
        if(component.getName()!=null) return component.getName();
        return null;
    }

    @Override
    public String base64head() {
        ProfileComponent component = item.getComponent(ProfileComponent.class);
        if(component==null) return null;
        if(component.getUniqueId()!=null) return component.getUniqueId().toString();
        return null;
    }

    @Override
    public String potionData() {
        PotionContentsComponent potion = item.getComponent(PotionContentsComponent.class);
        if(potion==null) return null;
        if(potion.getPotion()==null) return null;
        String potionNbt = potion.getPotion().name();
        String type = potionNbt.replace("LONG_","").replace("STRONG_", "");
        boolean extended = potionNbt.startsWith("LONG_");
        boolean upgraded = potionNbt.startsWith("STRONG_");
        return new PotionData(PotionData.Type.valueOf(type), extended, upgraded).toString();
    }

    @Override
    public String customPotion() {
        PotionContentsComponent potion = item.getComponent(PotionContentsComponent.class);
        if(potion==null) return null;
        if(potion.getCustomEffects()==null || potion.getCustomEffects().isEmpty()) return null;
        MobEffectInstance effect = potion.getCustomEffects().iterator().next(); // todo all effects
        MobEffect type = effect.getMobEffect();
        int duration = effect.getDetails().getDuration();
        int amplifier = effect.getDetails().getAmplifier();
        boolean ambient = effect.getDetails().isAmbient();
        boolean particles = effect.getDetails().isShowParticles();
        boolean icon = effect.getDetails().isShowIcon();
        return new PotionEffect(PotionType.byKey(type.name()), duration, amplifier, ambient, particles, icon).toString();
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
