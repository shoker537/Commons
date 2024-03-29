package ru.shk.velocity.commons;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.Protocol;
import dev.simplix.protocolize.api.Protocolize;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import ru.shk.commons.ServerType;
import ru.shk.commons.utils.CustomHead;
import ru.shk.commons.utils.Plugin;
import ru.shk.guilib.protocolize.packet.RenameItemPacket;
import ru.shk.mysql.connection.MySQL;
import ru.shk.velocity.commons.config.Config;
import ru.shk.velocity.commons.gui.GUILib;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Getter@Accessors(fluent = true)
//@Plugin(id = "commons", name = "Commons", authors = {"shoker137"}, version = "1.3.81", dependencies = {@Dependency(id = "mysqlapi")})
public class Commons {

    private MySQL mysql;
    private final Config config;
    private final ProxyServer proxy;
    private final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(15, new DefaultThreadFactory("Commons Main Pool"));

    @Accessors(fluent = false)@Getter private static Commons instance;

    private final HashMap<Integer, CustomHead> customHeadsCache = new HashMap<>();
    private final List<Plugin> plugins = new ArrayList<>();

    @Inject
    public Commons(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory){
        this.proxy = server;
        ru.shk.commons.utils.Logger.logger(logger);
        ServerType.setType(ServerType.VELOCITY);
        instance = this;
        config = Config.defaultConfig(dataDirectory.toFile());
        registerMessagingChannel("commons:updateinv");
        registerMessagingChannel("BungeeCord");
        plugins.add(new GUILib());

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
        mysql = new MySQL(config.getString("database","minigames"));
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
    }

    public void async(Runnable r){
        threadPool.submit(r);
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent e){
        threadPool.shutdown();
        try {
            if(!threadPool.awaitTermination(30, TimeUnit.SECONDS)) threadPool.shutdownNow();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
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
                case "executeAtBungee" -> {
                    String cmd = in.readUTF();
                    Player p = (Player) e.getTarget();
                    proxy.getCommandManager().executeAsync(p, cmd);
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
}
