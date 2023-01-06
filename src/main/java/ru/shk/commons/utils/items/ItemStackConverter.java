package ru.shk.commons.utils.items;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Nullable;
import ru.shk.commons.utils.items.universal.Enchantment;
import ru.shk.commons.utils.items.universal.EnchantmentType;
import ru.shk.commons.utils.items.universal.ItemFlag;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ItemStackConverter {
    private static final List<StringConverterRule> stringRules = new ArrayList<>();

    static {
        stringRules.add(new StringConverterRule(ConvertMaterial.ANY,"type", b -> b.type().name(), ItemStackBuilder::type));
        stringRules.add(new StringConverterRule(ConvertMaterial.ANY,"enchant", b -> enchantsToString(b.enchantments()), (b, s) -> b.enchant(enchantsFromString(s))));
        stringRules.add(new StringConverterRule(ConvertMaterial.ANY,"unbreakable", b -> String.valueOf(b.isUnbreakable()), (b, s) -> b.unbreakable(Boolean.parseBoolean(s))));
        stringRules.add(new StringConverterRule(ConvertMaterial.HEADS,"texture", ItemStackBuilder::base64head, ItemStackBuilder::base64head));
        stringRules.add(new StringConverterRule(ConvertMaterial.HEADS,"player", ItemStackBuilder::headOwnerName, ItemStackBuilder::headOwner));
        stringRules.add(new StringConverterRule(ConvertMaterial.HEADS,"player-uuid", b -> b.headOwnerUUID().toString(), (b, s) -> b.headOwner(UUID.fromString(s))));
        stringRules.add(new StringConverterRule(ConvertMaterial.HEADS,"custom-head", b -> b.customHeadId()==-1?null:String.valueOf(b.customHeadId()), (b, s) -> b.customHeadId(Integer.parseInt(s))));
        stringRules.add(new StringConverterRule(ConvertMaterial.ANY,"amount", b -> String.valueOf(b.amount()), (b, s) -> b.amount(Integer.parseInt(s))));
        stringRules.add(new StringConverterRule(ConvertMaterial.LEATHER_ARMOR,"leather-color", ItemStackBuilder::leatherColorAsHexString, (b, s) -> b.leatherColor(Color.decode(s))));
        stringRules.add(new StringConverterRule(ConvertMaterial.ANY,"hide-flags", b -> String.valueOf(ItemFlag.asInt(b.flags())), (b, s) -> b.flags(Integer.parseInt(s))));
        stringRules.add(new StringConverterRule(ConvertMaterial.ANY,"CMD", b -> String.valueOf(b.customModelData()), (b, s) -> b.customModelData(Integer.parseInt(s))));
    }

    private static String enchantsToString(List<Enchantment> list){
        if(list.size()==0) return "-";
        StringBuilder sb = new StringBuilder();
        for (Enchantment e : list) {
            sb.append(e.type().namespacedKey()).append(":").append(e.level()).append(",");
        }
        return sb.toString().substring(0, sb.length()-1);
    }

    private static List<Enchantment> enchantsFromString(String s){
        if(s.equals("-")) return new ArrayList<>();
        List<Enchantment> list = new ArrayList<>();
        String[] parts = s.split(",");
        for (String part : parts) {
            String[] p2 = part.split(":");
            String key = p2[0];
            int lvl = Integer.parseInt(p2[1]);
            list.add(new Enchantment(EnchantmentType.fromString(key), lvl));
        }
        return list;
    }

    public static ItemStackBuilder fromString(String s) {
        String material = s.split(" ")[0];
        HashMap<String, String> map = new LinkedHashMap<>();

        boolean first = true;
        for (String s1 : s.split(" ")) {
            if(first){
                first = false;
                continue;
            }
            try {
                String[] args = s1.split(":");
                map.put(args[0], args[1]);
            } catch (Throwable t){
                t.printStackTrace();
            }
        }

        ItemStackBuilder builder = ItemStackBuilder.newEmptyStack();
        builder.type(material.toUpperCase());
        map.forEach((type, value) -> {
            StringConverterRule rule = findRule(type);
            if(rule!=null && rule.convertMaterial.canApply(builder.type().name())){
                try {
                    rule.applyToStack.accept(builder, value);
                } catch (Throwable t){
                    t.printStackTrace();
                }
            }
        });

        return builder;
    }

    @Nullable
    private static StringConverterRule findRule(String name){
        return stringRules.stream().filter(stringConverterRule -> stringConverterRule.sectionName.equalsIgnoreCase(name)).findAny().orElse(null);
    }

    public static String toString(ItemStackBuilder b) {
        StringBuilder builder = new StringBuilder();
        for (StringConverterRule rule : stringRules) {
            if(!rule.convertMaterial.canApply(b.type().name())) continue;
            try {
                String v = rule.stringValue.apply(b);
                if(v==null) continue;
                builder.append(rule.sectionName).append(":").append(v);
            } catch (Throwable t){}
        }
        String result = builder.toString();
        return result.substring(0, result.length()-1);
    }

    @AllArgsConstructor
    private static class StringConverterRule {
        private final ConvertMaterial convertMaterial;
        private final String sectionName;
        private final Function<ItemStackBuilder, String> stringValue;
        private final BiConsumer<ItemStackBuilder, String> applyToStack;
    }

    private enum ConvertMaterial {
        ANY(null),
        LEATHER_ARMOR(List.of("LEATHER_CHESTPLATE", "LEATHER_BOOTS", "LEATHER_LEGGINGS", "LEATHER_HELMET")),
        HEADS(List.of("PLAYER_HEAD", "PLAYER_WALL_HEAD"));

        private final List<String> available;

        ConvertMaterial(List<String> available) {
            this.available = available;
        }

        public boolean canApply(String m){
            return available==null || available.contains(m);
        }
    }
}
