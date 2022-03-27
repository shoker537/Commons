package ru.shk.configapi;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import ru.shk.commons.Commons;
import ru.shk.commons.utils.Plugin;

import java.io.FileReader;
import java.nio.file.Paths;
import java.util.Properties;

public final class ConfigAPI implements Plugin {
    @Getter private static String serverName;

    @Override
    public void load() {
        Properties props = new Properties();
        try (var a = new FileReader(Paths.get("server.properties", new String[0]).toFile())) {
            props.load(a);
            serverName = props.getProperty("server-name");
        } catch (Exception e) {
            Commons.getInstance().warning("[ConfigAPI] Не указан \"server-name\" в server.properties! Какие-то плагины могут работать с ошибками.");
        }
    }

    @Override
    public void enable() {
    }

    @Override
    public void disable() {

    }
}
