package ru.shk.commons;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.sk89q.worldedit.WorldEdit;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import ru.shk.commons.sockets.SocketMessageListener;
import ru.shk.commons.sockets.low.ServerType;
import ru.shk.commons.sockets.low.SocketManager;
import ru.shk.commons.sockets.low.SocketMessageType;
import ru.shk.commons.utils.*;
import ru.shk.commons.utils.nms.PacketVersion;
import ru.shk.configapi.Config;
import ru.shk.configapi.ConfigAPI;
import ru.shk.guilib.GUILib;
import ru.shk.mysql.database.MySQL;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.net.InetSocketAddress;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Commons extends JavaPlugin {
    @Getter private static Commons instance;
    private static PacketVersion ver;
    private final List<Plugin> plugins = new ArrayList<>();
    private final HashMap<Integer, CustomHead> customHeadsCache = new HashMap<>();
    @Getter private MySQL mysql;
//    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(5, 15, 15L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    private final ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
    private final ThreadPoolExecutor teleportService = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
    @Getter@Nullable private WorldEditManager worldEditManager;
    @Getter private SocketManager socketManager;
    @Getter private PAFManager pafManager;
    private Config config;

    @Override
    public void onLoad() {
        SocketManager.serverType = ServerType.SPIGOT;
        pool.setKeepAliveTime(15, TimeUnit.SECONDS);
        info(" ");
        info(ChatColor.AQUA+"            shoker'"+ChatColor.WHITE+"s "+ChatColor.AQUA+"common"+ChatColor.WHITE+"s");
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        String ver = packageName.substring(packageName.lastIndexOf(46) + 1);
        try {
            Commons.ver = PacketVersion.valueOf(ver);
            info(ChatColor.WHITE+"          Running on "+ver+" - "+ChatColor.GREEN+"Supported");
        } catch (Exception e){
            Commons.ver = PacketVersion.values()[PacketVersion.values().length-1];
            info(ChatColor.WHITE+"          Running on "+ver+" - "+ChatColor.RED+"Unsupported version! "+ChatColor.GRAY+"Fallback version is "+Commons.ver.name());
        }
        info(" ");
        instance = this;
        try {
            plugins.add(new GUILib());
        } catch (Throwable e){
            e.printStackTrace();
        }
        try {
            plugins.add(new ConfigAPI());
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

    private void sendLocationFeedback(String uuid, Coordinates coordinates){
        ByteArrayDataOutput o = ByteStreams.newDataOutput();
        o.writeUTF("location");
        o.writeUTF(uuid);
        if(coordinates==null){
            o.writeUTF("player-not-found-error");
            o.writeInt(0);
            o.writeInt(0);
            o.writeInt(0);
        } else {
            o.writeUTF(coordinates.getWorld());
            o.writeInt(coordinates.getX());
            o.writeInt(coordinates.getY());
            o.writeInt(coordinates.getZ());
        }
        Bukkit.getOnlinePlayers().stream().findAny().ifPresent(player -> player.sendPluginMessage(this, "BungeeCord", o.toByteArray()));
    }

    public void executeCommandAtBungee(Player p, String cmd){
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("executeAtBungee");
        out.writeUTF(cmd);
        p.sendPluginMessage(this, "BungeeCord", out.toByteArray());
    }

    public void broadcastOnBungee(String msg){
        Optional<? extends Player> p = Bukkit.getOnlinePlayers().stream().findAny();
        if(p.isEmpty()) return;
        ByteArrayDataOutput o = ByteStreams.newDataOutput();
        o.writeUTF(msg);
        p.get().sendPluginMessage(this, "commons:broadcast", o.toByteArray());
    }

    public static PacketVersion getServerVersion(){
        return ver;
    }

    @Override
    public void onEnable() {
        config = new Config(getDataFolder(), true);
        if(!config.contains("sockets.enable")) config.setValue("sockets.enable", false);
        if(!config.contains("sockets.auto-find-port")) config.setValue("sockets.auto-find-port", true);
        if(!config.contains("sockets.server-port")) config.setValue("sockets.server-port", 3001);
        if(!config.contains("sockets.bungee-socket-ip")) config.setValue("sockets.bungee-socket-ip", "127.0.0.1");
        if(!config.contains("sockets.bungee-socket-port")) config.setValue("sockets.bungee-socket-port", 3000);
        if(config.getBoolean("sockets.enable")) {
            socketManager = new SocketManager(
                    config.getBoolean("sockets.auto-find-port")?-1:config.getInt("sockets.server-port"),
                    s -> sync(() -> getServer().getConsoleSender().sendMessage(colorize(s))),
                    new InetSocketAddress(config.getString("sockets.bungee-socket-ip"), config.getInt("sockets.bungee-socket-port"))
            );
            socketManager.getSocketThread().start();

            // TEST
            DecimalFormat f = new DecimalFormat("##.#");
            socketManager.getSocketMessageListeners().add(new SocketMessageListener("TPS") {
                @Override
                public void onMessage(SocketManager manager, SocketMessageType type, String channel, String server, DataInputStream data) {
                    manager.sendToBungee("TPS", List.of(f.format(MinecraftServer.getServer().recentTps[0]).toString()));
                }
            });
        }
        getServer().getPluginManager().registerEvents(new Events(), this);
        if(Bukkit.getPluginManager().getPlugin("MySQLAPI")==null){
            warning("&cMySQLAPI not loaded! &rSome features may be not available.");
        } else {
            Config config = new Config(getDataFolder(), true);
            if(config.contains("mysql-database")){
                info("Connecting to database "+org.bukkit.ChatColor.GREEN+config.getString("mysql-database"));
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
        pafManager = new PAFManager(this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "commons:broadcast");
        getServer().getMessenger().registerIncomingPluginChannel(this, "commons:location", (s, player, bytes) -> {
            async(() -> {
                ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
                String uuid = in.readUTF();
                UUID u = UUID.fromString(uuid);
                Player p = Bukkit.getPlayer(u);
                if(p==null || !p.isOnline()){
                    sendLocationFeedback(uuid, null);
                } else {
                    sendLocationFeedback(uuid, new Coordinates(p.getLocation()));
                }
            });
        });
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "commons:updateinv", (s, player, bytes) -> {
            ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
            String uuid = in.readUTF();
            UUID u = UUID.fromString(uuid);
            Player p = Bukkit.getPlayer(u);
            if(p==null || !p.isOnline()) return;
            p.updateInventory();
        });
        getServer().getMessenger().registerIncomingPluginChannel(this, "commons:generic", (channel, player, message) -> {
            try {
                ByteArrayDataInput in = ByteStreams.newDataInput(message);
                String type = in.readUTF();
                switch (type){
                    case "tp" -> teleportService.submit(() -> {
                        int teleportId = in.readInt();
                        String who = in.readUTF();
                        String to = in.readUTF();
                        Player p1;
                        int tries = 0;
                        do {
                            tries++;
                            if(tries==6) {
                                sync(() -> getLogger().warning("Player teleportation timed out - the first player is not on the server."));
                                return;
                            }
                            p1 = Bukkit.getPlayer(UUID.fromString(who));
                            if(p1==null){
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {}
                            }
                        } while (p1==null);
                        Player p2 = Bukkit.getPlayer(UUID.fromString(to));
                        if(p2==null) {
                            sync(() -> getLogger().warning("Tried to teleport player, but the second player is offline: "+to));
                            return;
                        }
                        Player finalP = p1;
                        sync(() -> finalP.teleport(p2));
                        p1.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GREEN+"Телепортирован!"));
                        sendTeleportFeedback(teleportId);
                    });
                }
            } catch (Throwable t){
                sync(t::printStackTrace);
            }
        });
    }

    private void sendTeleportFeedback(int id){
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("tpFeedback");
        out.writeInt(id);
        int tries = 0;
        while (Bukkit.getOnlinePlayers().size()==0) {
            tries++;
            if(tries==5) return;
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Bukkit.getOnlinePlayers().iterator().next().sendPluginMessage(this, "BungeeCord", out.toByteArray());
    }

    @Override
    public void onDisable() {
        if(socketManager!=null){
            socketManager.sendToBungee(SocketMessageType.UNREGISTER, List.of());
            try {
                socketManager.getSocketThread().getSendQueue().awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            socketManager.close();
        }
        plugins.forEach(plugin -> {
            try {
                plugin.disable();
            } catch (Exception e){
                e.printStackTrace();
            }
        });
        plugins.clear();
        info("Waiting for tasks to complete...");
        try {
            pool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        info("Tasks completed.");
    }

    public void showAdvancementNotification(Player p, String header, String footer, String icon){
//        new Notification(header, footer, "minecraft:"+icon).show(p);
    }

    @Nullable
    public ItemStackBuilder getCustomHead(int id){
        if(customHeadsCache.containsKey(id)) return new ItemStackBuilder(Material.PLAYER_HEAD).base64Head(customHeadsCache.get(id).getTexture());
        try (ResultSet rs = mysql.Query().SELECT("*").FROM("custom_heads").WHERE("id="+id).LIMIT(1).execute()) {
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
        try (ResultSet rs = mysql.Query().SELECT("*").FROM("custom_heads").WHERE("`key`='"+key+"'").LIMIT(1).execute()) {
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
    public static String colorizeWithHex(String message) {
        Pattern pattern = Pattern.compile("&#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String color = message.substring(matcher.start(), matcher.end());
            message = message.replace(color, ChatColor.of(color.substring(1)) + "");
            matcher = pattern.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
