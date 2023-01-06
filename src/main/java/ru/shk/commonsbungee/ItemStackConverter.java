package ru.shk.commonsbungee;

import dev.simplix.protocolize.data.ItemType;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Deprecated
public class ItemStackConverter {
    private static final List<StringConverterRule> stringRules = new ArrayList<>();

    static {
        stringRules.add(new StringConverterRule(ConvertMaterial.ANY,"type", b -> b.type().name(), (b, s) -> b.type(ItemType.valueOf(s.toUpperCase()))));
        stringRules.add(new StringConverterRule(ConvertMaterial.HEADS,"texture", ItemStackBuilder::base64head, ItemStackBuilder::base64head));
        stringRules.add(new StringConverterRule(ConvertMaterial.HEADS,"player", ItemStackBuilder::headOwner, ItemStackBuilder::headOwner));
//TODO:        stringRules.add(new StringConverterRule(ConvertMaterial.HEADS,"player-uuid", b -> b.skullOwnerUUID().toString(), (b, s) -> b.skullOwner(UUID.fromString(s))));
        stringRules.add(new StringConverterRule(ConvertMaterial.HEADS,"custom-head", b -> b.getCustomHeadId()==-1?null:String.valueOf(b.getCustomHeadId()), (b, s) -> b.customHead(Integer.parseInt(s))));
        stringRules.add(new StringConverterRule(ConvertMaterial.ANY,"amount", b -> String.valueOf(b.amount()), (b, s) -> b.amount(Integer.parseInt(s))));
//TODO:        stringRules.add(new StringConverterRule(ConvertMaterial.LEATHER_ARMOR,"leather-color", ItemStackBuilder::leatherColor, (b, s) -> b.leatherColor(Color.decode(s))));
        stringRules.add(new StringConverterRule(ConvertMaterial.ANY,"CMD", b -> String.valueOf(b.customModelData()), (b, s) -> b.customModelData(Integer.parseInt(s))));
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

        ItemStackBuilder builder = new ItemStackBuilder(ItemType.valueOf(material.toUpperCase()));
        map.forEach((type, value) -> {
            StringConverterRule rule = findRule(type);
            if(rule!=null && rule.convertMaterial.canApply(builder.type())){
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
            if(!rule.convertMaterial.canApply(b.type())) continue;
            try {
                builder.append(rule.sectionName+":"+rule.stringValue.apply(b));
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
        LEATHER_ARMOR(List.of(ItemType.LEATHER_CHESTPLATE, ItemType.LEATHER_BOOTS, ItemType.LEATHER_LEGGINGS, ItemType.LEATHER_HELMET)),
        HEADS(List.of(ItemType.PLAYER_HEAD));

        private final List<ItemType> available;

        ConvertMaterial(List<ItemType> available) {
            this.available = available;
        }

        public boolean canApply(ItemType m){
            return available==null || available.contains(m);
        }
    }
}
