package ru.shk.commons.utils;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.md_5.bungee.api.ChatColor;

public class Logger {
    @Setter@Accessors(fluent = true)
    private static java.util.logging.Logger logger;

    public static void info(String s){
        logger.info(s.replace('&', '§'));
    }
    public static void warning(String s){
        logger.warning(s.replace('&', '§'));
    }
}
