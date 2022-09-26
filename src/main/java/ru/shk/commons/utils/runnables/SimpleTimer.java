package ru.shk.commons.utils.runnables;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

public class SimpleTimer {
    private int seconds;
    private Consumer<Integer> action;
    private BukkitRunnable runnable;
    private Runnable done;
    private final JavaPlugin pl;

    public SimpleTimer(JavaPlugin pl){
        this.pl = pl;
    }

    public SimpleTimer totalTime(int seconds){
        this.seconds = seconds;
        return this;
    }

    public SimpleTimer onTick(Consumer<Integer> action){
        this.action = action;
        return this;
    }

    public SimpleTimer onDone(Runnable r){
        done = r;
        return this;
    }

    public void startSync(){
        runnable();
        runnable.runTaskTimer(pl, 0, 20);
    }

    public void startAsync(){
        runnable();
        runnable.runTaskTimerAsynchronously(pl, 0, 20);
    }

    public void cancel(){
        runnable.cancel();
    }

    public boolean isRunning(){
        return runnable!=null && !runnable.isCancelled();
    }

    private void runnable(){
        runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if(seconds==0){
                    cancel();
                    if(done!=null) done.run();
                    return;
                }
                action.accept(seconds);
                seconds--;
            }
        };
    }


}
