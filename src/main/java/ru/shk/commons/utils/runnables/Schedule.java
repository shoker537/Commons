package ru.shk.commons.utils.runnables;

import lombok.Setter;
import org.apache.logging.log4j.util.TriConsumer;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Schedule {
    @Setter private static Consumer<Runnable> sync;
    @Setter private static Consumer<Runnable> async;
    @Setter private static BiConsumer<Runnable, Duration> syncLater;
    @Setter private static BiConsumer<Runnable, Duration> asyncLater;
    @Setter private static TriConsumer<Runnable, Duration, Duration> syncRepeating;
    @Setter private static TriConsumer<Runnable, Duration, Duration> asyncRepeating;

    public static void sync(Runnable r){
        sync.accept(() -> {
            try {
                r.run();
            } catch (Throwable t){
                t.printStackTrace();
            }
        });
    }

    public static void async(Runnable r){
        async.accept(() -> {
            try {
                r.run();
            } catch (Throwable t){
                t.printStackTrace();
            }
        });
    }

    public static void syncLater(Runnable r, Duration delay){
        syncLater.accept(() -> {
            try {
                r.run();
            } catch (Throwable t){
                t.printStackTrace();
            }
        }, delay);
    }

    public static void asyncLater(Runnable r, Duration delay){
        asyncLater.accept(() -> {
            try {
                r.run();
            } catch (Throwable t){
                t.printStackTrace();
            }
        }, delay);
    }

    public static void syncRepeating(Runnable r, Duration delay, Duration period){
        syncRepeating.accept(() -> {
            try {
                r.run();
            } catch (Throwable t){
                t.printStackTrace();
            }
        }, delay, period);
    }

    public static void asyncRepeating(Runnable r, Duration delay, Duration period){
        asyncRepeating.accept(() -> {
            try {
                r.run();
            } catch (Throwable t){
                t.printStackTrace();
            }
        }, delay, period);
    }
}
