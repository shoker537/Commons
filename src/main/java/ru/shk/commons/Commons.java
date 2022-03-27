package ru.shk.commons;

import com.sk89q.worldedit.WorldEdit;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import ru.shk.commons.utils.*;
import ru.shk.commons.utils.nms.PacketVersion;
import ru.shk.configapi.Config;
import ru.shk.configapi.ConfigAPI;
import ru.shk.guilib.GUILib;
import ru.shk.mysql.database.MySQL;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public final class Commons extends JavaPlugin {
    @Getter private static Commons instance;
    private static PacketVersion ver;
    private final List<Plugin> plugins = new ArrayList<>();
    private final HashMap<Integer, CustomHead> customHeadsCache = new HashMap<>();
    private MySQL mysql;
    private final ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
    @Getter@Nullable private WorldEditManager worldEditManager;

    @Override
    public void onLoad() {
        info(" ");
        info(ChatColor.AQUA+"            shoker'"+ChatColor.WHITE+"s "+ChatColor.AQUA+"common"+ChatColor.WHITE+"s");
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        String ver = packageName.substring(packageName.lastIndexOf(46) + 1);
        try {
            Commons.ver = PacketVersion.valueOf(ver);
            info(ChatColor.WHITE+"          Running on "+ver+" - "+ChatColor.GREEN+"Supported");
        } catch (Exception e){
            Commons.ver = PacketVersion.values()[PacketVersion.values().length-1];
            info(ChatColor.WHITE+"          Running on "+ver+" - "+ChatColor.RED+"Unsupported! "+ChatColor.GRAY+"Fallback version is "+Commons.ver.name());
        }
        info(" ");
        instance = this;

        plugins.add(new GUILib());
        plugins.add(new ConfigAPI());

        plugins.forEach(plugin -> {
            try {
                plugin.load();
            } catch (Exception e){
                e.printStackTrace();
            }
        });
    }

    public static PacketVersion getServerVersion(){
        return ver;
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new Events(), this);
        if(Bukkit.getPluginManager().getPlugin("MySQLAPI")==null){
            warning("&cMySQLAPI not loaded! &rSome features may be not available.");
        } else {
            Config config = new Config(getDataFolder(), true);
            if(config.contains("mysql-database")){
                mysql = new MySQL(config.getString("mysql-database"));
            } else {
                warning("&cMySQL database is not defined in config! &rMySQL won't connect.");
            }
        }
        if(Bukkit.getPluginManager().getPlugin("WorldEdit")==null){
            warning("&cWorldEdit not found! &rSome API features may be not available.");
        } else {
            this.worldEditManager = new WorldEditManager(WorldEdit.getInstance());
        }
        plugins.forEach(plugin -> {
            try {
                plugin.enable();
            } catch (Exception e){
                e.printStackTrace();
            }
        });
    }
    @Override
    public void onDisable() {
        pool.shutdown();
        plugins.forEach(plugin -> {
            try {
                plugin.disable();
            } catch (Exception e){
                e.printStackTrace();
            }
        });
        plugins.clear();
    }

    @Nullable
    public ItemStackBuilder getCustomHead(int id){
        if(customHeadsCache.containsKey(id)) return new ItemStackBuilder(Material.PLAYER_HEAD).base64Head(customHeadsCache.get(id).getTexture());
        ResultSet rs = mysql.Query().SELECT("*").FROM("custom_heads").WHERE("id="+id).LIMIT(1).execute();
        try {
            if(rs.next()){
                CustomHead head = new CustomHead(id, rs.getString("key"), rs.getString("texture"));
                customHeadsCache.put(id, head);
                return new ItemStackBuilder(Material.PLAYER_HEAD).base64Head(head.getTexture());
            } else {
                throw new NullPointerException("No custom head with id "+id);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning(e.getMessage());
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
                return new ItemStackBuilder(Material.PLAYER_HEAD).base64Head(head.getTexture());
            } else {
                throw new NullPointerException("No custom head with key "+key);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static String secondsToTime(int s){
        int min = s/60;
        int sec = s - (min*60);
        return min+":"+(sec>9?sec:"0"+sec);
    }

    public static long currentSeconds(){
        return System.currentTimeMillis()/1000;
    }

    public static void registerEvents(JavaPlugin pl, Listener l){
        pl.getServer().getPluginManager().registerEvents(l, pl);
    }
    public void sync(Runnable r){
        getServer().getScheduler().runTask(this, r);
    }
    public void async(Runnable r){
        pool.submit(r);
    }
    public void syncLater(Runnable r, int delay){
        getServer().getScheduler().runTaskLater(this, r, delay);
    }
    public void asyncLater(Runnable r, int delay){
        getServer().getScheduler().runTaskLaterAsynchronously(this, r, delay);
    }
    public void syncRepeating(Runnable r, int delay, int period){
        getServer().getScheduler().runTaskTimer(this, r, delay, period);
    }
    public void asyncRepeating(Runnable r, int delay, int period){
        getServer().getScheduler().runTaskTimerAsynchronously(this, r, delay, period);
    }


    public void sync(JavaPlugin plugin, Runnable r){
        getServer().getScheduler().runTask(plugin, r);
    }
    public void syncLater(JavaPlugin plugin, Runnable r, int delay){
        getServer().getScheduler().runTaskLater(plugin, r, delay);
    }
    public void asyncLater(JavaPlugin plugin, Runnable r, int delay){
        getServer().getScheduler().runTaskLaterAsynchronously(plugin, r, delay);
    }
    public void syncRepeating(JavaPlugin plugin, Runnable r, int delay, int period){
        getServer().getScheduler().runTaskTimer(plugin, r, delay, period);
    }
    public void asyncRepeating(JavaPlugin plugin, Runnable r, int delay, int period){
        getServer().getScheduler().runTaskTimerAsynchronously(plugin, r, delay, period);
    }

    public void info(String log){
        Bukkit.getConsoleSender().sendMessage(colorize(log));
    }
    public void warning(String log){
        Bukkit.getConsoleSender().sendMessage(colorize("&c"+log));
    }
    public String colorize(String s){
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public void firework(Location l, Color color){
        Firework fw = (Firework) l.getWorld().spawnEntity(l, EntityType.FIREWORK);
        FireworkMeta fireworkMeta = fw.getFireworkMeta();
        FireworkEffect.Builder builder = FireworkEffect.builder();
        builder.withColor(color);
        builder.withFlicker();
        builder.with(FireworkEffect.Type.BALL_LARGE);
        builder.trail(true);
        builder.withFade(Color.AQUA);
        FireworkEffect effect = builder.build();
        fireworkMeta.addEffect(effect);
        fireworkMeta.setPower(0);
        fw.setFireworkMeta(fireworkMeta);
        fw.setMetadata("effect", new FixedMetadataValue(this, true));
        fw.detonate();
    }

}
