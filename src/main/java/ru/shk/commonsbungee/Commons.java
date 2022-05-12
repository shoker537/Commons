package ru.shk.commonsbungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import dev.simplix.protocolize.data.ItemType;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import ru.shk.commons.utils.CustomHead;
import ru.shk.guilibbungee.GUILib;
import ru.shk.mysql.database.MySQL;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Commons extends Plugin implements Listener {
    private ThreadPoolExecutor threadPool;
    @Getter private static Commons instance;
    @Getter private boolean isProtocolizeInstelled = false;
    private final List<ru.shk.commons.utils.Plugin> plugins = new ArrayList<>();
    private final HashMap<Integer, CustomHead> customHeadsCache = new HashMap<>();
    private MySQL mysql;

    @Override
    public void onLoad() {
        instance = this;
        info(" ");
        info("&b            shoker'&fs &bcommon&fs");
        info("&f                   v"+getDescription().getVersion());
        info(" ");
        if(getProxy().getPluginManager().getPlugin("Protocolize")==null) {
            warning("Protocolize not found! &fSome API features may be unavailable.");
        } else {
            isProtocolizeInstelled = true;
        }
        try {
            plugins.add(new GUILib());
        } catch (Throwable e){
            e.printStackTrace();
        }
        plugins.forEach(plugin -> {
            try {
                plugin.load();
            } catch (Throwable e){
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onEnable() {
        if(getProxy().getPluginManager().getPlugin("MySQLAPI")==null){
            warning("MySQLAPI not found! &fSome features may be unavailable.");
        } else {
            mysql = new MySQL("shield_bungee");
        }
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);

        getProxy().getPluginManager().registerListener(this, this);
        getProxy().registerChannel("commons:broadcast");
        plugins.forEach(plugin -> {
            try {
                plugin.enable();
            } catch (Throwable e){
                e.printStackTrace();
            }
        });
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e){
        if(!e.getTag().startsWith("commons")) return;
        String type = e.getTag().split(":")[1];
        switch (type) {
            case "broadcast" -> {
                ByteArrayDataInput in = ByteStreams.newDataInput(e.getData());
                String msg = in.readUTF();
                getProxy().broadcast(colorize(msg));
            }
        }
    }

    @Nullable
    public ItemStackBuilder getCustomHead(int id){
        if(customHeadsCache.containsKey(id)) return new ItemStackBuilder(ItemType.PLAYER_HEAD).base64head(customHeadsCache.get(id).getTexture());
        ResultSet rs = mysql.Query().SELECT("*").FROM("custom_heads").WHERE("id="+id).LIMIT(1).execute();
        try {
            if(rs.next()){
                CustomHead head = new CustomHead(id, rs.getString("key"), rs.getString("texture"));
                customHeadsCache.put(id, head);
                return new ItemStackBuilder(ItemType.PLAYER_HEAD).base64head(head.getTexture());
            } else {
                throw new NullPointerException("No custom head with id "+id);
            }
        } catch (Exception e) {
            getProxy().getLogger().warning(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    public ItemStackBuilder getCustomHead(String key){
        ResultSet rs = mysql.Query().SELECT("*").FROM("custom_heads").WHERE("key="+key).LIMIT(1).execute();
        try {
            if(rs.next()){
                CustomHead head = new CustomHead(rs.getInt("id"), key, rs.getString("texture"));
                return new ItemStackBuilder(ItemType.PLAYER_HEAD).base64head(head.getTexture());
            } else {
                throw new NullPointerException("No custom head with key "+key);
            }
        } catch (Exception e) {
            getProxy().getLogger().warning(e.getMessage());
            e.printStackTrace();
        }
        return null;
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



    public void registerEvents(Listener l){
        getProxy().getPluginManager().registerListener(this, l);
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
