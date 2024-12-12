package ru.shk.velocity.commons;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.Protocol;
import dev.simplix.protocolize.api.Protocolize;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.FastThreadLocalThread;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ru.shk.commons.ServerType;
import ru.shk.commons.utils.CustomHead;
import ru.shk.commons.utils.Plugin;
import ru.shk.commons.utils.gui.GUIManager;
import ru.shk.commons.utils.items.PlayerProcessor;
import ru.shk.commons.utils.items.universal.HeadsCache;
import ru.shk.commons.utils.runnables.Schedule;
import ru.shk.guilib.protocolize.packet.RenameItemPacket;
import ru.shk.mysql.connection.MySQL;
import ru.shk.mysql.connection.data.Rows;
import ru.shk.velocity.commons.cmd.FindCMD;
import ru.shk.velocity.commons.config.Config;
import ru.shk.velocity.commons.gui.GUILib;
import ru.shk.velocity.commons.utils.PluginMessage;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

@Getter@Accessors(fluent = true)
//@Plugin(id = "commons", name = "Commons", authors = {"shoker137"}, version = "1.3.81", dependencies = {@Dependency(id = "mysqlapi")})
public class Commons {
    private MySQL mysql;
    private final Config config;
    private final ProxyServer proxy;
    private final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(15, new DefaultThreadFactory("Commons Main Pool"));
    private final ThreadPoolExecutor singleThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1, new SingleThreadFactory("Commons Single Pool"));
    private final PlayerLocationReceiver playerLocationReceiver;

    private final ThreadPoolExecutor teleportService = new ThreadPoolExecutor(0, 5, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    private final HashMap<UUID, Future<?>> runningTeleports = new HashMap<>();
    private final List<Integer> tpInProcess = new ArrayList<>();
    private int lastTpId = 0;
    private final Object tpSyncObject = new Object();
    private PAFManager PAFManager;

    @Accessors(fluent = false)@Getter private static Commons instance;

    private final HashMap<Integer, CustomHead> customHeadsCache = new HashMap<>();
    private final List<Plugin> plugins = new ArrayList<>();

    private class SinglePoolThread extends FastThreadLocalThread {
        public SinglePoolThread(ThreadGroup group, Runnable target, String name) {
            super(group, target, name);
        }
    }
    private class SingleThreadFactory extends DefaultThreadFactory {

        public SingleThreadFactory(String poolName) {
            super(poolName);
        }

        @Override
        protected Thread newThread(Runnable r, String name) {
            return new SinglePoolThread(this.threadGroup, r, name);
        }
    }

    @Inject
    public Commons(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory){
        this.proxy = server;
        ServerType.setType(ServerType.VELOCITY);
        instance = this;
        config = Config.defaultConfig(dataDirectory.toFile());
        registerMessagingChannel("commons:updateinv");
        registerMessagingChannel("BungeeCord");
        plugins.add(new GUILib()); //todo: deprecated, remove
        plugins.add(new GUIManager());
        playerLocationReceiver = new PlayerLocationReceiver(this);
        plugins.forEach(plugin -> {
            try {
                plugin.load();
            } catch (Throwable t){
                t.printStackTrace();
            }
        });
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent e){
        proxy.getConsoleCommandSource().sendMessage(colorize(""));
        proxy.getConsoleCommandSource().sendMessage(colorize("            &bshoker'&fs &bcommon&fs"));
        proxy.getConsoleCommandSource().sendMessage(colorize("              for Velocity"));
        proxy.getConsoleCommandSource().sendMessage(colorize(""));
        mysql = new MySQL(config.getString("default-connection","minigames"));
        Protocolize.protocolRegistration().registerPacket(RenameItemPacket.MAPPINGS, Protocol.PLAY, PacketDirection.SERVERBOUND, RenameItemPacket.class);
        mysql.UpdateSync("CREATE TABLE IF NOT EXISTS `custom_heads` (" +
                "  `id` int NOT NULL AUTO_INCREMENT," +
                "  `key` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL," +
                "  `texture` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL," +
                "  PRIMARY KEY (`id`) USING BTREE," +
                "  UNIQUE KEY `UNIQUE` (`key`) USING BTREE" +
                ") ENGINE=InnoDB AUTO_INCREMENT=144 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
        plugins.forEach(plugin -> {
            try {
                plugin.enable();
            } catch (Throwable t){
                t.printStackTrace();
            }
        });
        PAFManager = new PAFManager(this);
        setupSchedule();
        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from("commons:generic"));
        proxy.getCommandManager().register(proxy.getCommandManager().metaBuilder("find").build(), new FindCMD(this));
    }

    private void setupSchedule(){
        Schedule.setAsync(this::async);
        Schedule.setSync(this::sync);
        Schedule.setAsyncLater(this::later);
        Schedule.setSyncLater((r, delay) -> later(() -> sync(r), delay));
        Schedule.setAsyncRepeating(this::repeat);
        Schedule.setSyncRepeating((r, delay, period) -> repeat(() -> sync(r), delay, period));
    }

    public void teleport(Player from, Player to){
        teleport(from, to, null);
    }

    public void teleport(Player from, Player to, @Nullable String targetPlayerServer){
        async(() -> {
            Optional<ServerConnection> fromServer = from.getCurrentServer();
            Optional<ServerConnection> toServer = to.getCurrentServer();
            if (toServer.isEmpty()) {
                from.sendMessage(Component.text("Не удалось телепортировать к "+to.getUsername()+": сервер игрока не найден.").color(NamedTextColor.RED));
                return;
            }
            if (targetPlayerServer!=null && !toServer.get().getServerInfo().getName().equals(targetPlayerServer)){
                from.sendMessage(Component.text("Не удалось телепортировать к "+to.getUsername()+": сервер игрока поменялся.").color(NamedTextColor.RED));
                return;
            }
            if(fromServer.map(s -> s.getServerInfo().getName()).orElse("").equals(toServer.get().getServerInfo().getName())){
                teleportAndWaitForFeedback(from, to);
            } else {
                from.sendActionBar(Component.text("Соединяем с сервером...").color(NamedTextColor.YELLOW));
                from.createConnectionRequest(toServer.get().getServer()).connectWithIndication().thenAcceptAsync(result -> {
                    if(result){
                        teleportAndWaitForFeedback(from, to);
                    } else {
                        from.sendActionBar(Component.text("Ошибка соединения. Попробуем ещё...").color(NamedTextColor.YELLOW));
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        from.createConnectionRequest(toServer.get().getServer()).connectWithIndication().thenAcceptAsync(result1 -> {
                            if(result1){
                                teleportAndWaitForFeedback(from, to);
                            } else {
                                from.sendActionBar(Component.text("Не удалось соединиться с сервером.").color(NamedTextColor.RED));
                                from.sendMessage(Component.text("Не удалось соединить вас с сервером "+toServer.get().getServerInfo()).color(NamedTextColor.RED));
                            }
                        });
                    }
                });
            }
        });
    }

    private void teleportAndWaitForFeedback(Player from, Player to){
        later(() -> {
            if(runningTeleports.containsKey(from.getUniqueId())){
                runningTeleports.get(from.getUniqueId()).cancel(true);
            }
            runningTeleports.put(from.getUniqueId(),
                    teleportService.submit(() -> {
                        from.sendMessage(Component.text("Телепортируем...").color(NamedTextColor.YELLOW));
                        int selectedTpId;
                        synchronized (tpSyncObject){
                            lastTpId++;
                            if(lastTpId==10000) lastTpId=1;
                            selectedTpId = lastTpId;
                        }
                        synchronized (tpInProcess){
                            tpInProcess.add(selectedTpId);
                        }
                        int times = 0;
                        do {
                            times++;
                            if(times==5){
                                warn("Не удалось подтвердить телепортацию: сервер не отправил ответ, время ожидания ответа истекло.");
                                return;
                            }
                            sendTeleportToServer(selectedTpId, from, to);
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                return;
                            }
                        } while (tpInProcess.contains(selectedTpId));
                    }));
        }, Duration.ofSeconds(3));
    }

    private void sendTeleportToServer(int tpId, Player who, Player to){
        to.getCurrentServer().ifPresent(s -> new PluginMessage("commons:generic")
                .writeUTF("tp")
                .writeInt(tpId)
                .writeUTF(who.getUniqueId().toString())
                .writeUTF(to.getUniqueId().toString())
                .send(s));
    }

    public void async(Runnable r){
        threadPool.submit(r);
    }
    public void sync(Runnable r){
        if (Thread.currentThread() instanceof SinglePoolThread) r.run(); else singleThreadPool.submit(r);
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent e){
        threadPool.shutdown();
        singleThreadPool.shutdown();
        try {
            if(!threadPool.awaitTermination(10, TimeUnit.SECONDS)) threadPool.shutdownNow();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        try {
            if(!singleThreadPool.awaitTermination(10, TimeUnit.SECONDS)) singleThreadPool.shutdownNow();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        plugins.forEach(plugin -> {
            try {
                plugin.disable();
            } catch (Throwable t){
                t.printStackTrace();
            }
        });
    }

    public void later(Runnable r, Duration delay){
        proxy.getScheduler().buildTask(this, r).delay(delay).schedule();
    }

    public void repeat(Runnable r, Duration delay, Duration period){
        proxy.getScheduler().buildTask(this, r).delay(delay).repeat(period).schedule();
    }

    public void registerMessagingChannel(String channel){
        if(channel.equalsIgnoreCase("bungeecord")) channel = "bungeecord:main";
        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from(channel));
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent e){
        String tag = ((MinecraftChannelIdentifier)e.getIdentifier()).getId();
        if(tag.equals("bungeecord:main")) {
            ByteArrayDataInput in = ByteStreams.newDataInput(e.getData());
            String type = in.readUTF();
            switch (type){
                case "executeAtBungee","executeAtProxy" -> {
                    String cmd = in.readUTF();
                    Player p = (Player) e.getTarget();
                    proxy.getCommandManager().executeAsync(p, cmd);
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
        if(!tag.startsWith("commons:")) return;
        String type = tag.split(":")[1];
        switch (type) {
            case "broadcast" -> {
                ByteArrayDataInput in = ByteStreams.newDataInput(e.getData());
                String msg = in.readUTF();
                proxy.sendMessage(colorize(msg));
            }
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onJoin(LoginEvent e){
        PlayerProcessor playerProcessor = HeadsCache.playerProcessor();
        if(playerProcessor==null) return;
        async(() -> {
            try {
                long id = playerProcessor.idFromUUID(e.getPlayer().getUniqueId());
                if(mysql.Query("SELECT id FROM heads_texture_cache WHERE player_id="+id+" AND updated_at > "+(System.currentTimeMillis()-259200000)+" LIMIT 1", Rows::next)) return;
                String texture = HeadsCache.getSkinTextureFromMojang(e.getPlayer().getUniqueId());
                if(texture==null) return;
                mysql.UpdateSync("INSERT INTO `heads_texture_cache` SET player_id="+id+", updated_at="+System.currentTimeMillis()+", texture='"+texture+"' ON DUPLICATE KEY UPDATE texture='"+texture+"', updated_at="+System.currentTimeMillis());
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
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
                e.printStackTrace();
            }
            return null;
        });
        if(head!=null) customHeadsCache.put(id, head);
        return head;
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
        if(head!=null) customHeadsCache.put(head.getId(), head);
        return head;
    }

    @Nullable
    public String getCustomHeadTexture(int id){
        CustomHead h = findCustomHead(id);
        if(h==null) return null;
        return h.getTexture();
    }

    @Nullable
    public String getCustomHeadTexture(String key){
        CustomHead h = findCustomHead(key);
        if(h==null) return null;
        return h.getTexture();
    }

    public static Component colorize(String s){
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }

    public void info(String s){
        ru.shk.commons.utils.Logger.info(s);
    }
    public void warn(String s){
        ru.shk.commons.utils.Logger.warning("§c"+s);
    }

    protected void sendFindPlayer(Player pp) {
        pp.getCurrentServer().ifPresent(s -> new PluginMessage("commons:location").writeUTF(pp.getUniqueId().toString()).send(s));
    }
}
