package ru.shk.commons.utils.items.universal;

import lombok.Getter;
import lombok.experimental.Accessors;

public enum PotionType {
    SPEED(1),
    SLOW(2, "slowness"),
    FAST_DIGGING(3, "haste"),
    SLOW_DIGGING(4,"mining_fatigue"),
    STRENGHT(5, "strength"),
    HEAL(6, "instant_health"),
    HARM(7, "instant_damage"),
    JUMP(8, "jump_boost"),
    NAUSEA(9),
    REGENERATION(10),
    RESISTANCE(11),
    FIRE_RESISTANCE(12),
    WATER_BREATHING(13),
    INVISIBILITY(14),
    BLINDNESS(15),
    NIGHT_VISION(16),
    HUNGER(17),
    WEAKNESS(18),
    POISON(19),
    WITHER(20),
    HEALTH_BOOST(21),
    ABSORPTION(22),
    SATURATION(23),
    GLOWING(24),
    LEVITATION(25),
    LUCK(26),
    UNLUCK(27),
    SLOW_FALLING(28),
    CONDUIT_POWER(29),
    DOLPHINS_GRACE(30),
    BAD_OMEN(31),
    HERO_OF_THE_VILLAGE(32),
    DARKNESS(33)
    ;

    @Getter@Accessors(fluent = true)
    private final int id;
    private final String minecraftKey;

    PotionType(int id){
        this(id, null);
    }

    PotionType(int id, String minecraftKey) {
        this.id = id;
        this.minecraftKey = minecraftKey;
    }

    public String minecraftKey(){
        return minecraftKey==null?name().toLowerCase():minecraftKey;
    }

    public static PotionType byKey(String key){
        for (PotionType value : values()) {
            if(value.minecraftKey.equalsIgnoreCase(key)) return value;
        }
        return null;
    }
    public static PotionType byId(int id){
        for (PotionType value : values()) {
            if(value.id==id) return value;
        }
        return null;
    }
}
