package ru.shk.configapibungee;

import lombok.Getter;
import lombok.SneakyThrows;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class Config {
    private final Configuration configuration;
    private static final ConfigurationProvider provider;
    private final File file;
    @Getter private final boolean existed;

    static {
        provider = ConfigurationProvider.getProvider(YamlConfiguration.class);
    }

    @SneakyThrows
    public Config(File file) {
        this.file = file;
        if (file.exists()) {
            existed = true;
        } else {
            existed = false;
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        configuration = provider.load(file);
    }

    public boolean contains(String s){
        return configuration.contains(s);
    }

    public Object get(String s){
        return configuration.get(s);
    }

    public String getString(String s){
        return configuration.getString(s);
    }

    public short getShort(String s){
        return configuration.getShort(s);
    }

    public float getFloat(String s){
        return configuration.getFloat(s);
    }

    public int getInt(String s){
        return configuration.getInt(s);
    }

    public double getDouble(String s){
        return configuration.getDouble(s);
    }

    public boolean getBoolean(String s){
        return configuration.getBoolean(s);
    }

    public List<String> getStringList(String s){
        return configuration.getStringList(s);
    }

    public Configuration getSection(String s){
        return configuration.getSection(s);
    }

    public Collection<String> getKeys(){
        return configuration.getKeys();
    }

    public void set(String key, Object value){
        configuration.set(key, value);
    }

    public void setAndSave(String key, Object value){
        configuration.set(key, value);
        save();
    }
    @SneakyThrows
    public void save(){
        provider.save(configuration, file);
    }


    public static void getIfHasInt(Configuration section, String key, Consumer<Integer> next){
        if(section.contains(key)) next.accept(section.getInt(key));
    }
    public static void getIfHasBoolean(Configuration section, String key, Consumer<Boolean> next){
        if(section.contains(key)) next.accept(section.getBoolean(key));
    }
    public static void getIfHasString(Configuration section, String key, Consumer<String> next){
        if(section.contains(key)) next.accept(section.getString(key));
    }
    public static void getIfHasStringList(Configuration section, String key, Consumer<List<String>> next){
        if(section.contains(key)) next.accept(section.getStringList(key));
    }
}
