package ru.shk.commons.utils.items.universal;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter@Accessors(fluent = true)
public class PotionEffect {
    private final PotionType type;
    private final int duration;
    private final int amplifier;
    private final boolean ambient;
    private final boolean particles;
    private final boolean icon;

    public PotionEffect(PotionType type, int duration, int amplifier, boolean ambient, boolean particles, boolean icon) {
        this.type = type;
        this.duration = duration;
        this.amplifier = amplifier;
        this.ambient = ambient;
        this.particles = particles;
        this.icon = icon;
    }

    @Override
    public String toString() {
        return "["+type.name().toLowerCase()+","+duration+","+amplifier+","+ambient+","+particles+","+icon+"]";
    }

    public static PotionEffect fromList(List<String> list){
        return new PotionEffect(PotionType.valueOf(list.get(0).toUpperCase()), Integer.parseInt(list.get(1)), Integer.parseInt(list.get(2)), Boolean.parseBoolean(list.get(3)), Boolean.parseBoolean(list.get(4)), Boolean.parseBoolean(list.get(5)));
    }
}
