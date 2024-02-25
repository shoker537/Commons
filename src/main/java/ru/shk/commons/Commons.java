package ru.shk.commons;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;
import com.sk89q.worldedit.WorldEdit;
import io.netty.util.concurrent.DefaultThreadFactory;
import land.shield.playerapi.CachedPlayer;
import lombok.Getter;
import lombok.SneakyThrows;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import ru.shk.commons.utils.*;
import ru.shk.commons.utils.items.universal.HeadsCache;
import ru.shk.commons.utils.nms.PacketVersion;
import ru.shk.configapi.Config;
import ru.shk.configapi.ConfigAPI;
import ru.shk.guilib.GUILib;
import ru.shk.mysql.connection.MySQL;

import javax.annotation.Nullable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Commons extends JavaPlugin {
    @Getter private static Commons instance;
    @Getter private static PacketVersion serverVersion;
    @Getter private static boolean isVersionLatestCompatible = false;
    private final List<Plugin> plugins = new ArrayList<>();
    private final ConcurrentHashMap<Integer, CustomHead> customHeadsCache = new ConcurrentHashMap<>();
    @Getter private MySQL mysql;
    final ThreadPoolExecutor pool = new ThreadPoolExecutor(5, 10, 5L, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(), new DefaultThreadFactory("Commons Main Pool"));
    private final ThreadPoolExecutor teleportService = (ThreadPoolExecutor) Executors.newFixedThreadPool(2, new DefaultThreadFactory("Commons Teleport Service Pool"));
    @Getter@Nullable private WorldEditManager worldEditManager;
    @Getter private PAFManager pafManager;
    @Getter private Config config;

    @Override
    public void onLoad() {
        Logger.logger(getLogger());
        ru.shk.commons.ServerType.setType(ru.shk.commons.ServerType.SPIGOT);
        info(" ");
        info(ChatColor.AQUA+"            shoker'"+ChatColor.WHITE+"s "+ChatColor.AQUA+"common"+ChatColor.WHITE+"s");
        String ver = Bukkit.getMinecraftVersion();
        Commons.serverVersion = PacketVersion.byName(ver);
        if(serverVersion==null){
            Commons.serverVersion = PacketVersion.values()[PacketVersion.values().length-1];
            info(ChatColor.WHITE+"          Running on "+ver+" - "+ChatColor.RED+"Unsupported NMS version! "+ChatColor.GRAY+"Fallback version is "+Commons.serverVersion.name());
            info(ChatColor.RED+"                          NMS features will not work properly.");
        } else {
            info(ChatColor.WHITE+"          Running on "+ver+" - "+ChatColor.GREEN+"Supported");
            if(Commons.serverVersion==PacketVersion.values()[PacketVersion.values().length-1]) isVersionLatestCompatible = true;
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

    public long getPlayerPlayedTime(String uuid){
        return mysql.QueryLong("SELECT time FROM BungeeOnlineTime WHERE uuid='"+uuid+"' LIMIT 1", 0);
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

    @Override
    public void onEnable() {
        config = new Config(getDataFolder(), true);
        if(!config.contains("sockets.enable")) config.setValue("sockets.enable", false);
        if(!config.contains("sockets.auto-find-port")) config.setValue("sockets.auto-find-port", true);
        if(!config.contains("sockets.server-port")) config.setValue("sockets.server-port", 3001);
        if(!config.contains("sockets.bungee-socket-ip")) config.setValue("sockets.bungee-socket-ip", "127.0.0.1");
        if(!config.contains("sockets.bungee-socket-port")) config.setValue("sockets.bungee-socket-port", 3000);
//        if(config.getBoolean("sockets.enable")) {
//            socketManager = new SocketManager(
//                    config.getBoolean("sockets.auto-find-port")?-1:config.getInt("sockets.server-port"),
//                    s -> sync(() -> getServer().getConsoleSender().sendMessage(colorize(s))),
//                    new InetSocketAddress(config.getString("sockets.bungee-socket-ip"), config.getInt("sockets.bungee-socket-port"))
//            );
//            socketManager.getSocketThread().start();
//
//            // TEST
//            DecimalFormat f = new DecimalFormat("##.#");
//            socketManager.getSocketMessageListeners().add(new SocketMessageListener("TPS") {
//                @Override
//                public void onMessage(SocketManager manager, SocketMessageType type, String channel, String server, DataInputStream data) {
//                    manager.sendToBungee("TPS", List.of(f.format(MinecraftServer.getServer().recentTps[0]).toString()));
//                }
//            });
//        }
        getServer().getScheduler().runTaskTimer(this, () -> {
            int players = Bukkit.getOnlinePlayers().size();
            if(players>60){
                pool.setMaximumPoolSize(30);
            } else if (players>30) {
                pool.setMaximumPoolSize(20);
            } else {
                pool.setMaximumPoolSize(10);
            }
        }, 1200, 1200);
        getServer().getPluginManager().registerEvents(new Events(), this);
        if(Bukkit.getPluginManager().getPlugin("MySQLAPI")==null){
            warning("&cMySQLAPI not loaded! &rSome features may be not available.");
        } else {
            Config config = new Config(getDataFolder(), true);
            if(config.contains("mysql-database")){
                info("Connecting to database "+org.bukkit.ChatColor.GREEN+config.getString("mysql-database"));
                mysql = new MySQL(config.getString("mysql-database"));
                HeadsCache.mysql(mysql);
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
        getCommand("commonsbukkit").setExecutor((sender, command, label, args) -> {
            sender.sendMessage(colorize(" &b          Commons v"+ getDescription().getVersion()));
            sender.sendMessage(colorize(" &bThreadPool active count: &f"+ pool.getActiveCount()));
            sender.sendMessage(colorize(" &bThreadPool queue count: &f"+ pool.getQueue().size()));
            sender.sendMessage(colorize(" &bThreadPool size: &f"+ pool.getPoolSize()));
            sender.sendMessage(colorize(" &bThreadPool maxSize: &f"+ pool.getMaximumPoolSize()));
            sender.sendMessage(colorize(" &bHeadsCache size: &f"+ ru.shk.commons.utils.items.ItemStackBuilder.headsCache().cacheSize()));
            return true;
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
//        if(socketManager!=null){
//            socketManager.sendToBungee(SocketMessageType.UNREGISTER, List.of());
//            try {
//                socketManager.getSocketThread().getSendQueue().awaitTermination(3, TimeUnit.SECONDS);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            socketManager.close();
//        }
        plugins.forEach(plugin -> {
            try {
                plugin.disable();
            } catch (Exception e){
                e.printStackTrace();
            }
        });
        plugins.clear();
        info("Waiting for tasks to complete... (queue size: "+pool.getQueue().size()+")");
        pool.shutdown();
        try {
            if(!pool.awaitTermination(30, TimeUnit.SECONDS)) pool.shutdownNow();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        info("Tasks completed.");
    }

    public void showAdvancementNotification(Player p, String header, String footer, String icon){
//        new Notification(header, footer, "minecraft:"+icon).show(p);
    }

    @Nullable@SneakyThrows
    public CustomHead findCustomHead(int id){
        if(customHeadsCache.containsKey(id)) return (customHeadsCache.get(id));
        CustomHead head = mysql.Query().SELECT("*").FROM("custom_heads").WHERE("id="+id).LIMIT(1).execute(rs -> {
            try {
                if(rs.next()){
                    return new CustomHead(id, rs.getString("key"), rs.getString("texture"));
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning(e.getMessage());
                e.printStackTrace();
            }
            return null;
        });
        if (head!=null) customHeadsCache.put(id, head);
        return head;
    }

    @Nullable
    public String getCustomHeadTexture(int id){
        CustomHead h = findCustomHead(id);
        if(h==null) return null;
        return h.getTexture();
    }


    @Nullable
    public String getSkinTexture(CachedPlayer cp){
        String texture = mysql.QueryString("SELECT texture FROM heads_texture_cache WHERE player_id="+cp.getId()+" AND "+System.currentTimeMillis()+"-updated_at<604800000 LIMIT 1;", null);
        if(texture!=null) texture = getSkinTextureFromMojang(cp.getUuid());
        if(texture==null) return null;
        mysql.UpdateAsync("INSERT INTO heads_texture_cache SET player_id="+cp.getId()+", texture='"+texture+"', updated_at="+System.currentTimeMillis()+" ON DUPLICATE KEY UPDATE texture='"+texture+"', updated_at="+System.currentTimeMillis());
        return texture;
    }

    private static String getSkinTextureFromMojang(UUID uuid) {
        try {
            String trimmedUUID = uuid.toString().replace("-", "");
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/"+trimmedUUID+"?unsigned=false");
            JsonObject o = new HTTPRequest(url).get().asJson();
            return o.getAsJsonArray("properties").get(0).getAsJsonObject().get("value").getAsString();
        } catch (Exception e){
            ru.shk.commonsbungee.Commons.getInstance().warning(e.getMessage());
            return null;
        }
    }

    @SneakyThrows public CustomHead findCustomHead(String key){
        Optional<CustomHead> h = customHeadsCache.values().stream().filter(customHead -> customHead.getKey().equals(key)).findAny();
        if(h.isPresent()) return h.get();
        CustomHead head = mysql.Query().SELECT("*").FROM("custom_heads").WHERE("`key`='"+key+"'").LIMIT(1).execute(rs -> {
            try {
                if(rs.next()){
                    return new CustomHead(rs.getInt("id"), key, rs.getString("texture"));
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning(e.getMessage());
                e.printStackTrace();
            }
            return null;
        });
        if (head!=null) customHeadsCache.put(head.getId(), head);
        return head;
    }

    @Nullable
    public String getCustomHeadTexture(String key){
        CustomHead h = findCustomHead(key);
        if(h==null) return null;
        return h.getTexture();
    }

    @Nullable@Deprecated
    public ItemStackBuilder getCustomHead(int id){
        return new ItemStackBuilder(Material.PLAYER_HEAD).customHead(id);
    }

    @Nullable@Deprecated
    public ItemStackBuilder getCustomHead(String key){
        return new ItemStackBuilder(Material.PLAYER_HEAD).customHead(key);
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

    public void firework(Location l, FireworkEffect.Builder builder){
        Firework fw = (Firework) l.getWorld().spawnEntity(l, EntityType.FIREWORK);
        FireworkMeta fireworkMeta = fw.getFireworkMeta();
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
