package ru.shk.configapi;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class Config extends YamlConfiguration {
    @Getter private final File file;

    public Config(File file){
        this(file.getParentFile(), file.getName());
    }

    public Config(File dataFolder, boolean config){
        this(dataFolder, "config");
    }

    public Config(File dataFolder, String filename){
        file = new File(dataFolder+File.separator+(filename.endsWith(".yml")?filename:filename+".yml"));
        if(!file.exists()){
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        load();
    }

    private void load(){
        try {
            load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public Location location(String name, Location var){
        if(contains(name)) return Config.decodeLocation(getString(name));
        set(name, var);
        save();
        return var;
    }

    public List<String> stringList(String name, List<String> var){
        if(contains(name)) return getStringList(name);
        set(name, var);
        save();
        return var;
    }

    public void setValue(String name, Object var){
        set(name, var);
        save();
    }

    public void save(){
        try {
            save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String encodeLocation(Location l) {
        return l.getWorld().getName() +
                " " +
                l.getBlockX() +
                " " +
                l.getBlockY() +
                " " +
                l.getBlockZ() +
                " " +
                (int) Math.floor(l.getYaw()) +
                " " +
                (int) Math.floor(l.getPitch());
    }

    public static Location decodeLocation(String s) {
        String[] ss = s.split(" ");
        World w = Bukkit.getWorld(ss[0]);
        if (w == null) {
            WorldCreator wc = new WorldCreator(ss[0]);
            w = wc.createWorld();
        }
        if(ss.length==4) return new Location(w, Double.parseDouble(ss[1]) + 0.5,
                Double.parseDouble(ss[2]) + 0.5, Double.parseDouble(ss[3]) + 0.5);
        return new Location(w, Double.parseDouble(ss[1]) + 0.5,
                Double.parseDouble(ss[2]) + 0.5, Double.parseDouble(ss[3]) + 0.5, Float.parseFloat(ss[4]),
                Float.parseFloat(ss[5]));
    }
    public static void getIfHasInt(ConfigurationSection section, String key, Consumer<Integer> next){
        if(section.contains(key)) next.accept(section.getInt(key));
    }
    public static void getIfHasBoolean(ConfigurationSection section, String key, Consumer<Boolean> next){
        if(section.contains(key)) next.accept(section.getBoolean(key));
    }
    public static void getIfHasString(ConfigurationSection section, String key, Consumer<String> next){
        if(section.contains(key)) next.accept(section.getString(key));
    }
    public static void getIfHasStringList(ConfigurationSection section, String key, Consumer<List<String>> next){
        if(section.contains(key)) next.accept(section.getStringList(key));
    }

}
