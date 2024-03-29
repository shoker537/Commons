package ru.shk.commonsbungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;
import dev.simplix.protocolize.data.ItemType;
import io.netty.util.concurrent.DefaultThreadFactory;
import land.shield.playerapi.CachedPlayer;
import lombok.Getter;
import lombok.SneakyThrows;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import ru.shk.commons.utils.CustomHead;
import ru.shk.commons.utils.HTTPRequest;
import ru.shk.commons.utils.Logger;
import ru.shk.commons.utils.TextComponentBuilder;
import ru.shk.commons.utils.items.universal.HeadsCache;
import ru.shk.commonsbungee.cmd.CommonsCmd;
import ru.shk.commonsbungee.cmd.CommonsTp;
import ru.shk.commonsbungee.cmd.ReloadChildPlugins;
import ru.shk.configapibungee.Config;
import ru.shk.guilibbungee.GUILib;
import ru.shk.mysql.connection.MySQL;

import javax.annotation.Nullable;
import java.net.URL;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Commons extends Plugin implements Listener {
    @Getter private ThreadPoolExecutor threadPool;
    private ThreadPoolExecutor teleportService;
    private final HashMap<UUID, Future<?>> runningTeleports = new HashMap<>();
    @Getter private static Commons instance;
    @Getter private boolean isProtocolizeInstalled = false;
    private final List<ru.shk.commons.utils.Plugin> plugins = new ArrayList<>();
    private final ConcurrentHashMap<Integer, CustomHead> customHeadsCache = new ConcurrentHashMap<>();
    private MySQL mysql;
    @Getter private PlayerLocationReceiver playerLocationReceiver;
    @Getter private PAFManager pafManager;
    @Getter private Config config;

    private final List<Integer> tpInProcess = new ArrayList<>();
    private int lastTpId = 0;

    @Override
    public void onLoad(){
        Logger.logger(getLogger());
        ru.shk.commons.ServerType.setType(ru.shk.commons.ServerType.BUNGEE);
        instance = this;
        info(" ");
        info("&b            shoker'&fs &bcommon&fs");
        info("&f                   v"+getDescription().getVersion());
        info(" ");
        if(getProxy().getPluginManager().getPlugin("Protocolize")==null) {
            warning("Protocolize not found! &fSome API features are unavailable.");
        } else {
            isProtocolizeInstalled = true;
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
        config = new Config(getDataFolder(), true);
//        if(!config.contains("sockets.enable")) config.setAndSave("sockets.enable", false);
//        if(!config.contains("sockets.server-port")) config.setAndSave("sockets.server-port", 3000);
        if(getProxy().getPluginManager().getPlugin("MySQLAPI")==null){
            warning("MySQLAPI not found! &fSome features may be unavailable.");
        } else {
            mysql = new MySQL(config.getString("database","shield_bungee"));
            HeadsCache.mysql(mysql);
        }
//        threadPool = new ThreadPoolExecutor(5, 10, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(5, new DefaultThreadFactory("Commons Main Pool"));
        threadPool.setKeepAliveTime(15, TimeUnit.SECONDS);
        teleportService = (ThreadPoolExecutor) Executors.newFixedThreadPool(2, new DefaultThreadFactory("Commons Teleport Service Pool"));
        teleportService.setKeepAliveTime(15, TimeUnit.SECONDS);
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().registerChannel("commons:updateinv");
        getProxy().registerChannel("commons:broadcast");
        getProxy().registerChannel("commons:location");
        getProxy().registerChannel("commons:generic");
        getProxy().registerChannel("commons:notification");
        syncRepeating(() -> {
            if(getProxy().getOnlineCount()<100) {
//                if(threadPool.getMaximumPoolSize()!=10) threadPool.setMaximumPoolSize(10);
                if(threadPool.getMaximumPoolSize()!=10) threadPool.setMaximumPoolSize(10);
                return;
            }
            if(getProxy().getOnlineCount()<150) {
//                threadPool.setMaximumPoolSize(15);
                if(threadPool.getMaximumPoolSize()!=15) threadPool.setMaximumPoolSize(15);
                return;
            }
            if(getProxy().getOnlineCount()<300) {
//                threadPool.setMaximumPoolSize(20);
                if(threadPool.getMaximumPoolSize()!=20) threadPool.setMaximumPoolSize(20);
                return;
            }
//            threadPool.setMaximumPoolSize(30+Math.min(30, getProxy().getOnlineCount()/40));
            if(threadPool.getMaximumPoolSize()!=30) threadPool.setMaximumPoolSize(30);
        }, 100, 60);
        playerLocationReceiver = new PlayerLocationReceiver(this);
        plugins.forEach(plugin -> {
            try {
                plugin.enable();
            } catch (Throwable e){
                e.printStackTrace();
            }
        });
        getProxy().getPluginManager().registerCommand(this, new ReloadChildPlugins());
        getProxy().getPluginManager().registerCommand(this, new CommonsCmd());
        getProxy().getPluginManager().registerCommand(this, new CommonsTp());
        if(getProxy().getPluginManager().getPlugin("PartyAndFriends")!=null) pafManager = new PAFManager(this);
    }

    public void teleport(ProxiedPlayer from, ProxiedPlayer to){
        async(() -> {
            if(from.getServer().getInfo().equals(to.getServer().getInfo())){
                teleportAndWaitForFeedback(from, to);
            } else {
                from.sendMessage(ChatMessageType.ACTION_BAR, new TextComponentBuilder("Соединяем с сервером...").withColor(ChatColor.YELLOW).build());
                from.connect(to.getServer().getInfo(), (result, error) -> {
                    if(result){
                        teleportAndWaitForFeedback(from, to);
                    } else {
                        from.sendMessage(ChatMessageType.ACTION_BAR, new TextComponentBuilder("Ошибка соединения с сервером. Пробуем ещё...").withColor(ChatColor.GOLD).build());
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        from.connect(to.getServer().getInfo(), (result1, error1) -> {
                            if(result1){
                                teleportAndWaitForFeedback(from, to);
                            } else {
                                from.sendMessage(new TextComponentBuilder("Error while teleporting: "+error1.getClass().getSimpleName()+" - "+error1.getMessage()).withColor(ChatColor.RED).build());
                                from.sendMessage(ChatMessageType.ACTION_BAR, new TextComponentBuilder("Не удалось соединить вас с сервером.").withColor(ChatColor.RED).build());
                            }
                        });
                    }
                });
            }
        });
    }

    private void teleportAndWaitForFeedback(ProxiedPlayer from, ProxiedPlayer to){
        syncLater(() -> {
            if(runningTeleports.containsKey(from.getUniqueId())){
                runningTeleports.get(from.getUniqueId()).cancel(true);
            }
            runningTeleports.put(from.getUniqueId(),
            teleportService.submit(() -> {
                from.sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.YELLOW+"Телепортируем..."));
                lastTpId++;
                if(lastTpId==10000) lastTpId=1;
                int selectedTpId = lastTpId;
                tpInProcess.add(selectedTpId);
                int times = 0;
                do {
                    times++;
                    if(times==4){
                        getLogger().warning("Не удалось подтвердить телепортацию: сервер не отправил ответ, время ожидания ответа истекло.");
                        return;
                    }
                    sendTeleportToServer(selectedTpId, from, to);
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        return;
                    }
                } while (tpInProcess.contains(selectedTpId));
            }));
        }, 3);
    }

    private void sendTeleportToServer(int tpId, ProxiedPlayer who, ProxiedPlayer to){
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("tp");
        out.writeInt(tpId);
        out.writeUTF(who.getUniqueId().toString());
        out.writeUTF(to.getUniqueId().toString());
        to.getServer().sendData("commons:generic", out.toByteArray());
    }

    public void showAdvancementNotification(ProxiedPlayer p, String header, String footer, String icon){
//        ByteArrayDataOutput o = ByteStreams.newDataOutput();
//        o.writeUTF(p.getUniqueId().toString());
//        o.writeUTF(header);
//        o.writeUTF(footer);
//        o.writeUTF(icon);
//        if(p.getServer()==null) return;
//        p.getServer().sendData("commons:notification", o.toByteArray());
    }

    @Nullable
    public String getSkinTexture(CachedPlayer cp){
        String texture = mysql.QueryString("SELECT texture FROM heads_texture_cache WHERE player_id="+cp.getId()+" AND "+System.currentTimeMillis()+"-updated_at<604800000 LIMIT 1;", null);
        if(texture==null) texture = getSkinTextureFromMojang(cp.getUuid());
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
            Commons.getInstance().warning(e.getMessage());
            return null;
        }
    }

    protected void sendFindPlayer(ProxiedPlayer pp){
        ByteArrayDataOutput o = ByteStreams.newDataOutput();
        o.writeUTF(pp.getUniqueId().toString());
        pp.getServer().sendData("commons:location", o.toByteArray());
    }

    public void sendInvUpdate(ProxiedPlayer p){
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(p.getUniqueId().toString());
        p.getServer().sendData("commons:updateinv", out.toByteArray());
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e){
        if(e.getTag().equals("BungeeCord")) {
            ByteArrayDataInput in = ByteStreams.newDataInput(e.getData());
            String type = in.readUTF();
            switch (type){
                case "executeAtBungee" -> {
                    String cmd = in.readUTF();
                    ProxiedPlayer p = (ProxiedPlayer) e.getReceiver();
                    getProxy().getPluginManager().dispatchCommand(p, cmd);
                }
                case "location" -> {
                    UUID uuid = UUID.fromString(in.readUTF());
                    String world = in.readUTF();
                    int x = in.readInt();
                    int y = in.readInt();
                    int z = in.readInt();
                    playerLocationReceiver.receivedLocation(uuid, world, x, y, z);
                }
                case "tpFeedback" -> {
                    int teleportId = in.readInt();
                    tpInProcess.removeIf(integer -> integer==teleportId);
                }
            }
            return;
        }
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

    @Nullable@SneakyThrows
    public CustomHead findCustomHead(int id){
        if(customHeadsCache.containsKey(id)) return (customHeadsCache.get(id));
        CustomHead head = mysql.Query().SELECT("*").FROM("custom_heads").WHERE("id="+id).LIMIT(1).execute(rs -> {
            try {
                if(rs.next()){
                    return new CustomHead(id, rs.getString("key"), rs.getString("texture"));
                }
            } catch (SQLException e){
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

    @SneakyThrows public CustomHead findCustomHead(String key){
        Optional<CustomHead> h = customHeadsCache.values().stream().filter(customHead -> customHead.getKey().equals(key)).findAny();
        if(h.isPresent()) return h.get();
        CustomHead head = mysql.Query().SELECT("*").FROM("custom_heads").WHERE("`key`='"+key+"'").LIMIT(1).execute(rs -> {
            try {
                if(rs.next()){
                    return new CustomHead(rs.getInt("id"), key, rs.getString("texture"));
                }
            } catch (Exception e) {
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

    @Nullable
    public ItemStackBuilder getCustomHead(int id){
        return new ItemStackBuilder(ItemType.PLAYER_HEAD).customHead(id);
    }

    @Nullable
    public ItemStackBuilder getCustomHead(String key){
        return new ItemStackBuilder(ItemType.PLAYER_HEAD).customHead(key);
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

    public long getPlayerPlayedTime(String uuid){
        return mysql.QueryLong("SELECT time FROM BungeeOnlineTime WHERE uuid='"+uuid+"' LIMIT 1", 0);
    }

    @Override
    public void onDisable() {
        info("Waiting for tasks to complete...");
        threadPool.shutdown();
        try {
            if(!threadPool.awaitTermination(30, TimeUnit.SECONDS)) threadPool.shutdownNow();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        info("Tasks completed.");
    }

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");

    public String getOnlineState(CachedPlayer cp){
        if(cp.getId()==-1) return ChatColor.RED+"Ошибка: id=-1";
        ProxiedPlayer pp = getProxy().getPlayer(cp.getUuid());
        if(pp!=null && pp.isConnected() && !isVanished(cp.getUuid())) return ChatColor.GREEN+"Онлайн"+ChatColor.WHITE+" на "+pp.getServer().getInfo().getName();
        long a = mysql.QueryLong("SELECT lastQuit FROM masuite_players WHERE id="+cp.getId()+" LIMIT 1", -1);
        if(a!=-1) return ChatColor.RED+"Заходил "+ formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(a), ZoneId.of("Europe/Moscow")));
        return ChatColor.RED+"Ошибка получения данных";
    }

    public boolean isVanished(UUID uuid){
        return mysql.QueryInt("SELECT Vanished FROM premiumvanish_playerdata WHERE uuid='"+uuid.toString()+"' LIMIT 1", 0)==1;
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
