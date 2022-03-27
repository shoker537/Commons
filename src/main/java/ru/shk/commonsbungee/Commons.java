package ru.shk.commonsbungee;

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Commons extends Plugin {
    private ThreadPoolExecutor threadPool;
    @Getter private static Commons instance;

    @Override
    public void onEnable() {
        instance = this;
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
    }

    public void registerEvents(Listener l){
        getProxy().getPluginManager().registerListener(this, l);
    }
    public void async(Runnable r){
        threadPool.submit(r);
    }
    public void syncLater(Runnable r, int delay){
        getProxy().getScheduler().schedule(this, r, delay, TimeUnit.SECONDS);
    }
    public void asyncLater(Runnable r, int delay){
        getProxy().getScheduler().schedule(this, () -> new Thread(r).start(), delay, TimeUnit.SECONDS);
    }
    public void syncRepeating(Runnable r, int delay, int period){
        getProxy().getScheduler().schedule(this, r, delay, period, TimeUnit.SECONDS);
    }
    public void asyncRepeating(Runnable r, int delay, int period){
        getProxy().getScheduler().schedule(this, () -> new Thread(r).start(), delay, period, TimeUnit.SECONDS);
    }
    public void info(String log){
        getLogger().info(colorize(log));
    }
    public void warning(String log){
        getLogger().warning(colorize(log));
    }
    public String colorize(String s){
        return ChatColor.translateAlternateColorCodes('&', s);
    }
    public static long currentSeconds(){
        return System.currentTimeMillis()/1000;
    }

    @Override
    public void onDisable() {
        threadPool.shutdown();
    }
}
