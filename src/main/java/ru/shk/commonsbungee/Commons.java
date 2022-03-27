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
    @Getter private boolean isProtocolizeInstelled = false;

    @Override
    public void onEnable() {
        info(" ");
        info("&b            shoker'&fs &bcommon&fs");
        info("&f                   v"+getDescription().getVersion());
        info(" ");
        if(getProxy().getPluginManager().getPlugin("Protocolize")==null) {
            warning("Protocolize not found! &fSome API features may be unavailable.");
        } else {
            isProtocolizeInstelled = true;
        }
        instance = this;
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
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


    public void registerEvents(Plugin plugin, Listener l){
        getProxy().getPluginManager().registerListener(plugin, l);
    }
    public void syncLater(Plugin plugin, Runnable r, int delay){
        getProxy().getScheduler().schedule(plugin, r, delay, TimeUnit.SECONDS);
    }
    public void asyncLater(Plugin plugin, Runnable r, int delay){
        getProxy().getScheduler().schedule(plugin, () -> new Thread(r).start(), delay, TimeUnit.SECONDS);
    }
    public void syncRepeating(Plugin plugin, Runnable r, int delay, int period){
        getProxy().getScheduler().schedule(plugin, r, delay, period, TimeUnit.SECONDS);
    }
    public void asyncRepeating(Plugin plugin, Runnable r, int delay, int period){
        getProxy().getScheduler().schedule(plugin, () -> new Thread(r).start(), delay, period, TimeUnit.SECONDS);
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
