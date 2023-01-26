package ru.shk.velocity.config;

import lombok.Getter;
import lombok.SneakyThrows;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.File;
import java.io.IOException;

public class Config extends YamlFile {
    @Getter private final File file;

    public Config(File dataFolder, String configName){
        this(new File(dataFolder+File.separator+(configName.endsWith(".yml")?configName:configName+".yml")));
    }

    public Config(File configFile){
        this.file = configFile;
        if(!file.exists()){
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            load(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setAndSave(String key, Object value){
        set(key, value);
        save();
    }

    @SneakyThrows
    public void save(){
        save(file);
    }

    public static Config defaultConfig(File dataFolder){
        return new Config(dataFolder, "config");
    }
}
