package ru.shk.commons.sockets.low;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.shk.commons.sockets.SocketMessageListener;
import ru.shk.commons.sockets.SocketThread;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class SocketManager {
    @Getter private final Consumer<String> logConsumer;
    public static ServerType serverType;
    @Getter private SocketThread socketThread;
    @Getter private final String currentServerName;
    @Getter private final List<SocketMessageListener> socketMessageListeners = new ArrayList<>();
    @Getter private final List<SocketServerInfo> backendServers = new ArrayList<>();

    public SocketManager(int listenPort, Consumer<String> logConsumer) {
        this.logConsumer = logConsumer;
        this.currentServerName = serverType.serverName();
        if(listenPort==-1){
            for (int i = 3001; i < 3050; i++) {
                socketThread = new SocketThread(i, this);
                if (socketThread.isServerStarted()) break;
            }
        } else {
            this.socketThread = new SocketThread(listenPort, this);
        }
        if(!socketThread.isServerStarted()) logConsumer.accept("&cServer could not start up :(");
    }

    public SocketManager(int listenPort, Consumer<String> logConsumer, InetSocketAddress bungee){
        this(listenPort, logConsumer);
        backendServers.add(new SocketServerInfo(bungee, "BungeeCord"));
    }

    public void sendData(SocketServerInfo adress, SocketMessageType type, int data){
        sendData(adress, type, List.of(data));
    }

    public void sendData(SocketServerInfo adress, SocketMessageType type, String data){
        sendData(adress, type, List.of(data));
    }

    public void sendData(String serverName, SocketMessageType type, List<Object> data){
        Optional<SocketServerInfo> info = getSocketServerInfo(serverName);
        if (info.isEmpty()) throw new IllegalStateException("Server "+serverName+" is not registered!");
        socketThread.sendData(info.get(), type, null, data);
    }

    public Optional<SocketServerInfo> getSocketServerInfo(String serverName){
        return backendServers.stream().filter(socketServerInfo -> socketServerInfo.getName().equals(serverName)).findAny();
    }

    public void sendData(String serverName, String channel, List<Object> data){
        Optional<SocketServerInfo> info = getSocketServerInfo(serverName);
        if (info.isEmpty()) throw new IllegalStateException("Server "+serverName+" is not registered!");
        socketThread.sendData(info.get(), SocketMessageType.SEVERE, channel, data);
    }

    public void sendData(SocketServerInfo adress, SocketMessageType type, List<Object> data){
        socketThread.sendData(adress, type, null, data);
    }

    public void sendData(SocketServerInfo adress, String channel, List<Object> data){
        socketThread.sendData(adress, SocketMessageType.SEVERE, channel, data);
    }

    public void close(){
        socketThread.close();
    }

    public void sendToBungee(SocketMessageType type, List<Object> data){
        if(type==SocketMessageType.SEVERE) throw new IllegalStateException("SEVERE can not be used in this method without channel property");
        Optional<SocketServerInfo> info = getSocketServerInfo("BungeeCord");
        if (info.isEmpty()) throw new IllegalStateException("Server BungeeCord is not registered!");
        socketThread.sendData(info.get(), type, null, data);
    }
    public void sendToBungee(@NotNull String channel, List<Object> data){
        Optional<SocketServerInfo> info = getSocketServerInfo("BungeeCord");
        if (info.isEmpty()) throw new IllegalStateException("Server BungeeCord is not registered!");
        socketThread.sendData(info.get(), SocketMessageType.SEVERE, channel, data);
    }
}