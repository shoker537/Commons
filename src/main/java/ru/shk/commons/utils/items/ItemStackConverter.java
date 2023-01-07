package ru.shk.commons.utils.items;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.Nullable;
import ru.shk.commons.utils.items.universal.Enchantment;
import ru.shk.commons.utils.items.universal.EnchantmentType;
import ru.shk.commons.utils.items.universal.ItemFlag;
import ru.shk.commons.utils.items.universal.parse.type.StringListValue;
import ru.shk.commons.utils.items.universal.parse.type.StringValue;
import ru.shk.commons.utils.items.universal.parse.type.Value;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ItemStackConverter {
    private static final List<StringConverterRule> stringRules = new ArrayList<>();

    static {
        stringRules.add(new StringConverterRule(ConvertMaterial.ANY,"type", b -> new StringValue().value(b.type().name()), (b, value) -> b.type(value.stringValue())));
        stringRules.add(new StringConverterRule(ConvertMaterial.ANY,"name", b -> new StringValue().value(b.displayName()), (b, value) -> b.displayName(value.stringValue())));
        stringRules.add(new StringConverterRule(ConvertMaterial.ANY,"lore", b -> new StringListValue().value(b.lore()), (b, value) -> b.lore(((StringListValue)value).value())));
        stringRules.add(new StringConverterRule(ConvertMaterial.ANY,"enchant", b -> new StringListValue().value(enchantsToStringList(b.enchantments())), (b, s) -> b.enchant(enchantsFromString((StringListValue) s))));
        stringRules.add(new StringConverterRule(ConvertMaterial.ANY,"unbreakable", b -> new StringValue().value(String.valueOf(b.isUnbreakable())), (b, s) -> b.unbreakable(Boolean.parseBoolean(s.stringValue()))));
        stringRules.add(new StringConverterRule(ConvertMaterial.HEADS,"texture", b -> new StringValue().value(b.base64head()), (b, value) -> b.base64head(value.stringValue())));
        stringRules.add(new StringConverterRule(ConvertMaterial.HEADS,"player", b -> new StringValue().value(b.headOwnerName()), (b, value) -> b.headOwner(value.stringValue())));
        stringRules.add(new StringConverterRule(ConvertMaterial.HEADS,"custom-head", b -> new StringValue().value(b.customHeadId()==-1?null:String.valueOf(b.customHeadId())), (b, s) -> b.customHead(Integer.parseInt(s.stringValue()))));
        stringRules.add(new StringConverterRule(ConvertMaterial.ANY,"amount", b -> new StringValue().value(String.valueOf(b.amount())), (b, s) -> b.amount(Integer.parseInt(s.stringValue()))));
        stringRules.add(new StringConverterRule(ConvertMaterial.LEATHER_ARMOR,"leather-color", b -> new StringValue().value(b.leatherColorAsHexString()), (b, s) -> b.leatherColor(Color.decode(s.stringValue()))));
        stringRules.add(new StringConverterRule(ConvertMaterial.ANY,"hide-flags", b -> new StringValue().value(String.valueOf(ItemFlag.asInt(b.flags()))), (b, s) -> b.flags(Integer.parseInt(s.stringValue()))));
        stringRules.add(new StringConverterRule(ConvertMaterial.ANY,"CMD", b -> new StringValue().value(String.valueOf(b.customModelData())), (b, s) -> b.customModelData(Integer.parseInt(s.stringValue()))));
    }

    private static List<String> enchantsToStringList(List<Enchantment> list){
        if(list.size()==0) return null;
        List<String> list1 = new ArrayList<>();
        for (Enchantment e : list) {
            list1.add(e.type().namespacedKey()+":"+e.level());
        }
        return list1;
    }

    private static List<Enchantment> enchantsFromString(StringListValue s){
        List<Enchantment> list = new ArrayList<>();
        s.value().forEach(part -> {
            String[] p2 = part.split(":");
            String key = p2[0];
            int lvl = Integer.parseInt(p2[1]);
            list.add(new Enchantment(EnchantmentType.fromString(key), lvl));
        });
        return list;
    }


    private static List<Value<?>> values(String line){
        line+=" ";
        char[] lineArray = line.toCharArray();
        List<Value<?>> list = new ArrayList<>();
        boolean isInsideTextBlock = false;
        boolean isList = false;
        boolean isInsideList = false;
        boolean isEscapingNextChar = false;
        boolean isInsideValue = false;

        StringBuilder typing = new StringBuilder();
        String name = null;

        for (char nextChar : lineArray) {
            if (nextChar == '\\' && !isEscapingNextChar) {
                isEscapingNextChar = true;
                continue;
            }
            if(!isEscapingNextChar && nextChar=='[' && !isInsideTextBlock){
                isList = true;
                isInsideList = true;
                continue;
            }
            if(!isEscapingNextChar && nextChar==']' && !isInsideTextBlock){
                isInsideList = false;
                continue;
            }
            if(isInsideList){
                if(nextChar=='\"' && !isEscapingNextChar) {
                    isInsideTextBlock = !isInsideTextBlock;
                }
                typing.append(nextChar);
                isEscapingNextChar = false;
                continue;
            }
            if (nextChar == ':' && !isInsideValue && !isList) {
                isInsideValue = true;
                name = typing.toString();
                typing = new StringBuilder();
                isEscapingNextChar = false;
                continue;
            }
            if (!isInsideValue) {
                typing.append(nextChar);
                isEscapingNextChar = false;
                continue;
            }
            if (nextChar == ' ' && !isInsideTextBlock) {
                isInsideValue = false;
                Value<?> value;
                if(isList){
                    value = new StringListValue().name(name).stringValue(typing.toString());
                } else {
                    value = new StringValue().name(name).stringValue(typing.toString());
                }
                list.add(value);
                isList = false;
                typing = new StringBuilder();
                isEscapingNextChar = false;
                continue;
            }
            if (nextChar == '\"' && !isEscapingNextChar) {
                isInsideTextBlock = !isInsideTextBlock;
                continue;
            }
            isEscapingNextChar = false;
            typing.append(nextChar);
        }

        return list;
    }

    public static ItemStackBuilder fromString(String s) {
        if(!s.endsWith(" ")) s+=" ";
        String material = s.split(" ")[0];
        String valuesString = s.replace(material+" ", "");
        if(valuesString.length()==0) return ItemStackBuilder.newEmptyStack().type(material);
        List<Value<?>> values = values(valuesString);
        ItemStackBuilder builder = ItemStackBuilder.newEmptyStack();
        builder.type(material.toUpperCase());
        for (Value<?> value : values) {
            StringConverterRule rule = findRule(value.name());
            if(rule==null) {
                continue;
            }
            if(value.value()==null){
                continue;
            }
            rule.applyToStack.accept(builder, value);
        }

        return builder;
    }

    @Nullable
    private static StringConverterRule findRule(String name){
        for (StringConverterRule rule : stringRules) {
            boolean a = name.equalsIgnoreCase(rule.sectionName);
            if(a){
                return rule;
            } else {
            }
        }
        return null;
    }

    public static String toString(ItemStackBuilder b) {
        StringBuilder builder = new StringBuilder();
        for (StringConverterRule rule : stringRules) {
            if(!rule.convertMaterial.canApply(b.type().name())) continue;
            try {
                Value<?> v = rule.stringValue.apply(b);
                if(v==null) continue;
                builder.append(rule.sectionName).append(":").append(v.stringValue());
            } catch (Throwable t){t.printStackTrace();}
        }
        String result = builder.toString();
        return result.substring(0, result.length()-1);
    }

    @AllArgsConstructor
    private static class StringConverterRule {
        private final ConvertMaterial convertMaterial;
        private final String sectionName;
        private final Function<ItemStackBuilder, Value<?>> stringValue;
        private final BiConsumer<ItemStackBuilder, Value<?>> applyToStack;
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
