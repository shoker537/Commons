package ru.shk.commons.sockets;

import lombok.Getter;
import ru.shk.commons.sockets.low.SocketManager;
import ru.shk.commons.sockets.low.SocketMessageType;
import ru.shk.commons.sockets.low.SocketServerInfo;

import java.io.DataInputStream;

public abstract class SocketMessageListener {
    @Getter private final String channel;

    public SocketMessageListener(String channel) {
        this.channel = channel;
    }

    public SocketMessageListener(){
        this(null);
    }

    public abstract void onMessage(SocketManager manager, SocketMessageType type, String channel, String server, DataInputStream data);

    public void register(SocketManager socketManager){
        socketManager.getSocketMessageListeners().add(this);
    }

    public void unregister(SocketManager socketManager){
        socketManager.getSocketMessageListeners().remove(this);
    }
    public void register(){
        SocketManager.serverType.defaultSocketManager().getSocketMessageListeners().add(this);
    }

    public void unregister(){
        SocketManager.serverType.defaultSocketManager().getSocketMessageListeners().remove(this);
    }
}
