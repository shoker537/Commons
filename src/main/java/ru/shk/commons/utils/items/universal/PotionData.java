package ru.shk.commons.utils.items.universal;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter@Accessors(fluent = true)
public class PotionData {
    private final Type type;
    private boolean extended = false;
    private boolean upgraded = false;

    public PotionData(Type type) {
        this.type = type;
    }

    public PotionData(Type type, boolean extended, boolean upgraded) {
        this.type = type;
        this.extended = extended;
        this.upgraded = upgraded;
    }

    public enum Type {
        UNCRAFTABLE,
        WATER,
        MUNDANE,
        THICK,
        AWKWARD,
        NIGHT_VISION,
        INVISIBILITY,
        JUMP,
        FIRE_RESISTANCE,
        SPEED,
        SLOWNESS,
        WATER_BREATHING,
        INSTANT_HEAL,
        INSTANT_DAMAGE,
        POISON,
        STRENGTH,
        WEAKNESS,
        LUCK,
        TURTLE_MASTER,
        SLOW_FALLING
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(type.name());
        if(extended) b.append(",extended");
        if(upgraded) b.append(",upgraded");
        return "["+b.toString()+"]";
    }

    public static PotionData fromList(List<String> list){
        String type = "UNCRAFTABLE";
        boolean extend = false;
        boolean upgrade = false;

        for (int i = 0; i < list.size(); i++) {
            if(i==0){
                type = list.get(0);
                continue;
            }
            String a = list.get(i);
            if(a.equalsIgnoreCase("extended")) {
                extend = true;
                continue;
            }
            if(a.equalsIgnoreCase("upgraded")) upgrade = true;
        }
        return new PotionData(Type.valueOf(type.toUpperCase()), extend, upgrade);
    }
}
