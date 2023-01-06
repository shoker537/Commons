package ru.shk.commons;

import lombok.Setter;

public enum ServerType {
    SPIGOT,
    BUNGEE;

    @Setter private static ServerType type;
    public static ServerType get(){return type;}
}
