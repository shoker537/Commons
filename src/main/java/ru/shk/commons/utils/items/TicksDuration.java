package ru.shk.commons.utils.items;

import java.time.Duration;

public class TicksDuration {
    public static Duration of(int ticks){
        return Duration.ofMillis(ticks * 50L);
    }
    public static int of(Duration d){
        return (int) (d.toMillis() / 50);
    }
}
