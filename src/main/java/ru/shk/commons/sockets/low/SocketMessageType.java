package ru.shk.commons.sockets.low;

import lombok.Getter;

public enum SocketMessageType {
    SEVERE(1),
    PING(2),
    PONG(3),
    REGISTER(4),
    UNREGISTER(5);

    public static String END_MESSAGE = "/end/";

    @Getter private final byte id;

    SocketMessageType(int id) {
        this.id = (byte) id;
    }

    public static SocketMessageType fromId(byte id){
        for (SocketMessageType value : values()) {
            if(value.id==id) return value;
        }
        return null;
    }
}
