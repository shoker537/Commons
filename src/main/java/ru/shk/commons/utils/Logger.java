package ru.shk.commons.utils;

import lombok.Setter;
import lombok.experimental.Accessors;

public class Logger {
    @Setter@Accessors(fluent = true)
    private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger("Commons");

    public static void info(String s){
        logger.info(traceInfo()+" "+s.replace('&', '§'));
    }
    public static void warning(String s){
        logger.warning(traceInfo()+" "+s.replace('&', '§'));
    }

    private static String traceInfo(){
        StackTraceElement e = Thread.currentThread().getStackTrace()[3];
        String className = e.getClassName();
        String[] splitClassName = className.split("\\.");
        String classSimpleName = splitClassName.length>1?splitClassName[splitClassName.length - 1]:splitClassName[0];
        return "["+classSimpleName+"#"+e.getMethodName()+"] ";
    }
}
