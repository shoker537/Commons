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
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.JedisPooled;
import ru.shk.commons.utils.*;
import ru.shk.commons.utils.gui.GUIManager;
import ru.shk.commons.utils.items.universal.HeadsCache;
import ru.shk.commons.utils.nms.PacketVersion;
import ru.shk.commons.utils.redis.RedisCredentials;
import ru.shk.commons.utils.redis.channels.ChannelListener;
import ru.shk.commons.utils.runnables.Schedule;
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
    @Getter@Nullable private WorldEditManager worldEditManager;
    @Getter private PAFManager pafManager;
    @Getter private Config config;
    public static final String REDIS_GENERAL_CHANNEL = "commons:general";

    @Getter private JedisPooled jedis;
    private ChannelListener redisListener;
    private boolean isFolia = false;

    @Override
    public void onLoad() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {}
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
        boolean folia = isFolia();
        if (folia){
            warning("          Folia - limited mode!");
        }
        info(" ");
        instance = this;
        try {
            plugins.add(new GUIManager());
        } catch (Throwable e){
            e.printStackTrace();
        }
        if (!folia) {
            try {
                plugins.add(new GUILib());
            } catch (Throwable e){
                e.printStackTrace();
            }
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
        setupSchedule();
    }

    private void setupSchedule(){
        Schedule.setSync(this::sync);
        Schedule.setSyncLater((r, delay) -> syncLater(() -> sync(r), (int)(delay.toMillis()/50)));
        Schedule.setSyncRepeating((r, delay, period) -> syncRepeating(r, (int)(delay.toMillis()/50), (int)(period.toMillis()/50)));
        Schedule.setAsync(this::async);
        Schedule.setAsyncLater((r, d) -> asyncLater(r, (int)(d.toMillis()/50)));
        Schedule.setAsyncRepeating((r, d, p) -> asyncRepeating(r, (int)(d.toMillis()/50), (int)(p.toMillis()/50)));
    }

    public long getPlayerPlayedTime(String uuid){
        return mysql.QueryLong("SELECT time FROM BungeeOnlineTime WHERE uuid='"+uuid+"' LIMIT 1", 0);
    }

    private void sendLocationFeedback(String uuid, Coordinates coordinates){
        Schedule.async(() -> {
            JsonObject o = new JsonObject();
            o.addProperty("type", "locationfeedback");
            o.addProperty("playerUUID", uuid);
            o.addProperty("world", coordinates.getWorld());
            o.addProperty("x", coordinates.getX());
            o.addProperty("y", coordinates.getY());
            o.addProperty("z", coordinates.getZ());
            jedis.publish(REDIS_GENERAL_CHANNEL, o.toString());
        });
    }

    public void executeCommandAtProxy(Player p, String cmd){
        JsonObject o = new JsonObject();
        o.addProperty("type", "executeAtProxy");
        o.addProperty("playerUUID", p.getUniqueId().toString());
        o.addProperty("command",cmd);
        jedis.publish(REDIS_GENERAL_CHANNEL, o.toString());
    }

    public void messageAtProxy(Player p, String msg){
        JsonObject o = new JsonObject();
        o.addProperty("type", "message");
        o.addProperty("playerUUID", p.getUniqueId().toString());
        o.addProperty("message",msg);
        jedis.publish(REDIS_GENERAL_CHANNEL, o.toString());
    }

    public void broadcastOnProxy(String msg){
        broadcastOnProxy(msg, null);
    }

    public void broadcastOnProxy(String msg, String permission){
        JsonObject o = new JsonObject();
        o.addProperty("type", "broadcastAtProxy");
        o.addProperty("message",msg);
        if (permission!=null) o.addProperty("permission", permission);
        jedis.publish(REDIS_GENERAL_CHANNEL, o.toString());
    }

    @Override
    public void onEnable() {
        config = new Config(getDataFolder(), true);
        if (config.getBoolean("redis.enabled", true)) {
            RedisCredentials redisCredentials = new RedisCredentials(config.getString("redis.host", "127.0.0.1"), config.getInt("redis.port", 6379), config.getString("redis.user"), config.getString("redis.password"));
            RedisCredentials.DEFAULT = redisCredentials;
            if (config.getBoolean("redis.use-general-channel", true)) {
                jedis = redisCredentials.newJedis(1,1,1);
                redisListener = new ChannelListener(redisCredentials, REDIS_GENERAL_CHANNEL, this::onRedisMessage);
            }
        }
        getServer().getGlobalRegionScheduler().runAtFixedRate(this, task -> {
            int players = Bukkit.getOnlinePlayers().size();
            if(players>90){
                pool.setMaximumPoolSize(20);
            } else if (players>30) {
                pool.setMaximumPoolSize(10);
            } else {
                pool.setMaximumPoolSize(5);
            }
        }, 1200, 1200);
        getServer().getPluginManager().registerEvents(new Events(), this);
        if(Bukkit.getPluginManager().getPlugin("MySQLAPI")==null){
            warning("&cMySQLAPI not loaded! &rSome features may be not available.");
        } else {
            Config config = new Config(getDataFolder(), true);
            if(config.contains("default-connection")){
                info("Connecting to "+org.bukkit.ChatColor.GREEN+config.getString("default-connection"));
                mysql = new MySQL(config.getString("default-connection"));
                HeadsCache.mysql(mysql);
            } else {
                warning("&cMySQL connection is not defined in config! &rMySQL won't connect.");
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
            if (args.length==0){
                sender.sendMessage(colorize(" &b          Commons v" + getDescription().getVersion()));
                sender.sendMessage(colorize(" &bThreadPool active count: &f"+ pool.getActiveCount()));
                sender.sendMessage(colorize(" &bThreadPool queue count: &f"+ pool.getQueue().size()));
                sender.sendMessage(colorize(" &bThreadPool size: &f"+ pool.getPoolSize()));
                sender.sendMessage(colorize(" &bThreadPool maxSize: &f"+ pool.getMaximumPoolSize()));
                sender.sendMessage(colorize(" &bHeadsCache size: &f"+ ru.shk.commons.utils.items.ItemStackBuilder.headsCache().cacheSize()));
                return true;
            }
            if (args[0].equalsIgnoreCase("debugclose")){
                GUIManager.instance().setDebugClose(!GUIManager.instance().isDebugClose());
                sender.sendMessage(colorize(" &bDebug close is now &f"+ GUIManager.instance().isDebugClose()));
            }
            return true;
        });
        pafManager = new PAFManager(this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "commons:updateinv", (s, player, bytes) -> {
            ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
            String uuid = in.readUTF();
            UUID u = UUID.fromString(uuid);
            Player p = Bukkit.getPlayer(u);
            if(p==null || !p.isOnline()) return;
            p.updateInventory();
        });
    }

    private void onRedisMessage(JsonObject o){
        switch (o.get("type").getAsString()){
            case "teleportrequest" -> {
                int tpId = o.get("tpId").getAsInt();
                UUID playerUUID = UUID.fromString(o.get("playerUUID").getAsString());
                UUID toUUID = UUID.fromString(o.get("toUUID").getAsString());
                sync(() -> {
                    Player p = Bukkit.getPlayer(playerUUID);
                    if (p==null) return;
                    Player to = Bukkit.getPlayer(toUUID);
                    if (to==null) return;
                    if (to.getVehicle()==null){
                        p.teleportAsync(to.getLocation());
                    } else {
                        p.teleportAsync(to.getLocation().clone().add(0,1,0));
                    }
                    async(() -> sendTeleportFeedback(tpId));
                });
            }
            case "locationrequest" -> {
//                getLogger().info("New location request: "+o.toString());
                UUID uuid = UUID.fromString(o.get("playerUUID").getAsString());
                sync(() -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p!=null) sendLocationFeedback(p.getUniqueId().toString(), new Coordinates(p.getLocation()));
                });
            }
        }
    }

    private void sendTeleportFeedback(int id){
        JsonObject o = new JsonObject();
        o.addProperty("type","teleportfeedback");
        o.addProperty("tpId", id);
        jedis.publish(REDIS_GENERAL_CHANNEL, o.toString());
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
        if (jedis!=null) {
            try {
                jedis.close();
            } catch (Throwable t){
                t.printStackTrace();
            }
        }
        if (redisListener!=null) redisListener.shutdown();
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

    private boolean isFolia() {
        return isFolia;
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
        return secondsToTime(s, false);
    }
    public static String secondsToTime(int s, boolean fullMinutes){
        int min = s/60;
        int sec = s - (min*60);
        return (min>9 || !fullMinutes ? min : "0"+min)+":"+(sec>9?sec:"0"+sec);
    }

    public static long currentSeconds(){
        return System.currentTimeMillis()/1000;
    }

    public static void registerEvents(JavaPlugin pl, Listener l){
        pl.getServer().getPluginManager().registerEvents(l, pl);
    }
    public void sync(Runnable r){
        if (isFolia()) {
            getServer().getGlobalRegionScheduler().run(this, task -> r.run());
            return;
        }
        if (Bukkit.isPrimaryThread()) r.run(); else getServer().getScheduler().runTask(this, r);
    }
    public void async(Runnable r){
        pool.submit(r);
    }
    public void syncLater(Runnable r, int delay){
        if (isFolia()) {
            getServer().getGlobalRegionScheduler().runDelayed(this, task -> r.run(), delay);
            return;
        }
        getServer().getScheduler().runTaskLater(this, r, delay);
    }
    public void asyncLater(Runnable r, int delay){
        getServer().getScheduler().runTaskLaterAsynchronously(this, r, delay);
    }
    public void syncRepeating(Runnable r, int delay, int period){
        if (isFolia()){
            getServer().getGlobalRegionScheduler().runAtFixedRate(this, task -> r.run(), delay, period);
            return;
        }
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
        Firework fw = (Firework) l.getWorld().spawnEntity(l, EntityType.FIREWORK_ROCKET);
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
        Firework fw = (Firework) l.getWorld().spawnEntity(l, EntityType.FIREWORK_ROCKET);
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
