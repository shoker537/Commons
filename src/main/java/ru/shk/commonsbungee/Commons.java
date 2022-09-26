package ru.shk.commonsbungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;
import dev.simplix.protocolize.data.ItemType;
import land.shield.playerapi.CachedPlayer;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import ru.shk.commons.sockets.SocketMessageListener;
import ru.shk.commons.sockets.low.SocketManager;
import ru.shk.commons.sockets.low.SocketMessageType;
import ru.shk.commons.sockets.low.SocketServerInfo;
import ru.shk.commons.utils.CustomHead;
import ru.shk.commons.utils.HTTPRequest;
import ru.shk.commons.sockets.low.ServerType;
import ru.shk.commonsbungee.cmd.ReloadChildPlugins;
import ru.shk.configapibungee.Config;
import ru.shk.guilibbungee.GUILib;
import ru.shk.mysql.database.MySQL;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Commons extends Plugin implements Listener {
    @Getter private ThreadPoolExecutor threadPool;
    @Getter private static Commons instance;
    @Getter private boolean isProtocolizeInstelled = false;
    private final List<ru.shk.commons.utils.Plugin> plugins = new ArrayList<>();
    private final HashMap<Integer, CustomHead> customHeadsCache = new HashMap<>();
    private MySQL mysql;
    @Getter private PlayerLocationReceiver playerLocationReceiver;
    @Getter private PAFManager pafManager;
    @Getter private SocketManager socketManager;
    private Config config;

    @Override
    public void onLoad(){
        SocketManager.serverType = ServerType.BUNGEE;
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
        config = new Config(getDataFolder(), true);
        if(!config.contains("sockets.enable")) config.setAndSave("sockets.enable", false);
        if(!config.contains("sockets.server-port")) config.setAndSave("sockets.server-port", 3000);
        if(config.getBoolean("sockets.enable")) {
            socketManager = new SocketManager(config.getInt("sockets.server-port"), s -> getLogger().info(colorize(s)));
            socketManager.getSocketThread().start();
            socketManager.getSocketMessageListeners().add(new SocketMessageListener("TPS") {
                @Override
                public void onMessage(SocketManager manager, SocketMessageType type, String channel, String server, DataInputStream data) {
                    try {
                        getLogger().warning(" TPS on "+server+" is "+data.readUTF());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            asyncRepeating(() -> socketManager.getBackendServers().forEach(socketServerInfo -> {
                if(!socketServerInfo.getName().equals("BungeeCord")) {
                    getLogger().warning("Sending TPS request to "+socketServerInfo.getName()+" at "+socketServerInfo.getAddress().getPort());
                    socketManager.sendData(socketServerInfo, "TPS", List.of());
                }
            }), 10,10);
        }
        if(getProxy().getPluginManager().getPlugin("MySQLAPI")==null){
            warning("MySQLAPI not found! &fSome features may be unavailable.");
        } else {
            mysql = new MySQL("shield_bungee");
        }
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(30);
        threadPool.setKeepAliveTime(5, TimeUnit.SECONDS);

        getProxy().getPluginManager().registerListener(this, this);
        getProxy().registerChannel("commons:updateinv");
        getProxy().registerChannel("commons:broadcast");
        getProxy().registerChannel("commons:location");
        getProxy().registerChannel("commons:notification");
        playerLocationReceiver = new PlayerLocationReceiver(this);
        plugins.forEach(plugin -> {
            try {
                plugin.enable();
            } catch (Throwable e){
                e.printStackTrace();
            }
        });
        getProxy().getPluginManager().registerCommand(this, new ReloadChildPlugins());
        if(getProxy().getPluginManager().getPlugin("PartyAndFriends")!=null) pafManager = new PAFManager(this);
    }

    public void showAdvancementNotification(ProxiedPlayer p, String header, String footer, String icon){
        ByteArrayDataOutput o = ByteStreams.newDataOutput();
        o.writeUTF(p.getUniqueId().toString());
        o.writeUTF(header);
        o.writeUTF(footer);
        o.writeUTF(icon);
        if(p.getServer()==null) return;
        p.getServer().sendData("commons:notification", o.toByteArray());
    }

    @Nullable
    public String getSkinTexture(CachedPlayer cp){
        ResultSet rs = mysql.Query("SELECT texture FROM heads_texture_cache WHERE player_id="+cp.getId()+" AND "+System.currentTimeMillis()+"-updated_at<604800000 LIMIT 1;");
        try {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        String texture = getSkinTextureFromMojang(cp.getUuid());
        if(texture==null) return null;
        mysql.Update("INSERT INTO heads_texture_cache SET player_id="+cp.getId()+", texture='"+texture+"', updated_at="+System.currentTimeMillis()+" ON DUPLICATE KEY UPDATE texture='"+texture+"', updated_at="+System.currentTimeMillis());
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
                case "CPAF" -> {
                    if(pafManager!=null) async(() -> pafManager.acceptPluginMessage((ProxiedPlayer) e.getReceiver(),in));
                }
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
        ResultSet rs = mysql.Query().SELECT("*").FROM("custom_heads").WHERE("`key`='"+key+"'").LIMIT(1).execute();
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
        info("Waiting for tasks to complete...");
        try {
            threadPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        info("Tasks completed.");
    }

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");

    public String getOnlineState(CachedPlayer cp){
        if(cp.getId()==-1) return ChatColor.RED+"Ошибка: id=-1";
        ProxiedPlayer pp = getProxy().getPlayer(cp.getUuid());
        if(pp!=null && pp.isConnected() && !isVanished(cp.getUuid())) return ChatColor.GREEN+"Онлайн"+ChatColor.WHITE+" на "+pp.getServer().getInfo().getName();
        ResultSet rs = mysql.Query().SELECT("lastQuit").FROM("masuite_players").WHERE("id="+cp.getId()).LIMIT(1).execute();
        try {
            if(rs.next()){
                long a = rs.getLong(1)*1000;
                return ChatColor.RED+"Заходил "+ formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(a), ZoneId.of("Europe/Moscow")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ChatColor.RED+"Ошибка получения данных";
    }

    public boolean isVanished(UUID uuid){
        ResultSet rs = mysql.Query("SELECT Vanished FROM premiumvanish_playerdata WHERE uuid='"+uuid.toString()+"' LIMIT 1");
        try {
            if(rs.next()){
                return rs.getInt(1)==1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
