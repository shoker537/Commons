package ru.shk.commons.utils;

import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;
import ru.shk.commons.Commons;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Deprecated
public class ItemStackConverter {
    private static final List<StringConverterRule> stringRules = new ArrayList<>();

    static {
        stringRules.add(new StringConverterRule(ConvertMaterial.ANY,"type", b -> b.type().name(), (b, s) -> b.type(Material.valueOf(s.toUpperCase()))));
        stringRules.add(new StringConverterRule(ConvertMaterial.HEADS,"texture", ItemStackBuilder::base64Head, ItemStackBuilder::base64Head));
        stringRules.add(new StringConverterRule(ConvertMaterial.HEADS,"player", ItemStackBuilder::skullOwner, ItemStackBuilder::skullOwner));
        stringRules.add(new StringConverterRule(ConvertMaterial.HEADS,"player-uuid", b -> b.skullOwnerUUID().toString(), (b, s) -> b.skullOwner(UUID.fromString(s))));
        stringRules.add(new StringConverterRule(ConvertMaterial.HEADS,"custom-head", b -> b.getCustomHeadId()==-1?null:String.valueOf(b.getCustomHeadId()), (b, s) -> b.customHead(Integer.parseInt(s))));
        stringRules.add(new StringConverterRule(ConvertMaterial.ANY,"amount", b -> String.valueOf(b.count()), (b, s) -> b.count(Integer.parseInt(s))));
        stringRules.add(new StringConverterRule(ConvertMaterial.LEATHER_ARMOR,"leather-color", ItemStackBuilder::leatherColor, (b, s) -> b.leatherColor(Color.decode(s))));
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

//        Commons.getInstance().sync(() -> Commons.getInstance().info(map.keySet().stream().map(s1 -> "Stack: "+s1+map.get(s1)).collect(Collectors.joining(","))));

        ItemStackBuilder builder = new ItemStackBuilder(Material.valueOf(material.toUpperCase()));
        map.forEach((type, value) -> {
            StringConverterRule rule = findRule(type);
//            if(rule!=null) Commons.getInstance().sync(() -> Commons.getInstance().info("Checking rule "+rule.sectionName+"("+material+"): CanApply:"+rule.convertMaterial.canApply(builder.type())));
            if(rule!=null && rule.convertMaterial.canApply(builder.type())){
                try {
                    rule.applyToStack.accept(builder, value);
                } catch (Throwable t){
                    Commons.getInstance().sync(t::printStackTrace);
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
                String v = rule.stringValue.apply(b);
                if(v==null) continue;
                builder.append(rule.sectionName+":"+v);
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
        LEATHER_ARMOR(List.of(Material.LEATHER_CHESTPLATE, Material.LEATHER_BOOTS, Material.LEATHER_LEGGINGS, Material.LEATHER_HELMET)),
        HEADS(List.of(Material.PLAYER_HEAD, Material.PLAYER_WALL_HEAD));

        private final List<Material> available;

        ConvertMaterial(List<Material> available) {
            this.available = available;
        }

        public boolean canApply(Material m){
            return available==null || available.contains(m);
        }
    }
}
