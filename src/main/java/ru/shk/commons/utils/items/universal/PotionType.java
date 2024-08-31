package ru.shk.commons.utils.items.universal;

import lombok.Getter;
import lombok.experimental.Accessors;

public enum PotionType {
    SPEED(),
    SLOW("slowness"),
    FAST_DIGGING("haste"),
    SLOW_DIGGING("mining_fatigue"),
    STRENGHT("strength"),
    HEAL("instant_health"),
    HARM("instant_damage"),
    JUMP("jump_boost"),
    NAUSEA(),
    REGENERATION(),
    RESISTANCE(),
    FIRE_RESISTANCE(),
    WATER_BREATHING(),
    INVISIBILITY(),
    BLINDNESS(),
    NIGHT_VISION(),
    HUNGER(),
    WEAKNESS(),
    POISON(),
    WITHER(),
    HEALTH_BOOST(),
    ABSORPTION(),
    SATURATION(),
    GLOWING(),
    LEVITATION(),
    LUCK(),
    UNLUCK(),
    SLOW_FALLING(),
    CONDUIT_POWER(),
    DOLPHINS_GRACE(),
    BAD_OMEN(),
    HERO_OF_THE_VILLAGE(),
    DARKNESS(),
    WIND_CHARGED(),
    WEAVING(),
    TRIAL_OMEN(),
    INFESTED(),
    OOZING(),
    RAID_OMEN()
    ;

    @Getter@Accessors(fluent = true)
    private final String minecraftKey;
    PotionType (){
        this.minecraftKey = name().toLowerCase();
    }

    PotionType(String minecraftKey) {
        this.minecraftKey = minecraftKey;
    }

    public String minecraftKey(){
        return minecraftKey==null?name().toLowerCase():minecraftKey;
    }

    public static PotionType byKey(String key){
        for (PotionType value : values()) {
            if(value.minecraftKey.equalsIgnoreCase(key) || value.name().equalsIgnoreCase(key)) return value;
        }
        return null;
    }
}
