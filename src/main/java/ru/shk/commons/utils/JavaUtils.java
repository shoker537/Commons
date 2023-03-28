package ru.shk.commons.utils;

import joptsimple.util.RegexMatcher;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaUtils {

    public static long msFromUserInput(String s){
        try {
            long ms = 0;
            Pattern pattern = Pattern.compile("(\\d+[a-zA-Z])");
            Matcher m = pattern.matcher(s);
            while (m.find()){
                String group = m.group();
                String lastChar = group.substring(group.length()-1);
                Integer value = Integer.parseInt(group.substring(0, group.length()-1));
                switch (lastChar){
                    case "y" -> ms+=value*31536000000L;
                    case "M" -> ms+=value*2592000000L;
                    case "d" -> ms+=value*86400000;
                    case "h" -> ms+=value*3600000;
                    case "m" -> ms+=value*60000;
                    case "s" -> ms+=value*1000;
                }
            }
            return ms;
        } catch (Exception e){
            return -1;
        }
    }

    public static UUID uuidFromNoDashString(String s){
        return UUID.fromString(s.replaceAll(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                "$1-$2-$3-$4-$5"));
    }
}
