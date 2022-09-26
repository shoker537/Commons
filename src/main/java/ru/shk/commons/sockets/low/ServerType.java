package ru.shk.commons.sockets.low;

import ru.shk.commons.sockets.SocketThread;

import java.io.DataInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

public enum ServerType {
    SPIGOT,
    BUNGEE;

    private static final List<SocketMessageType> SPIGOT_RESPOND_TYPES = List.of(SocketMessageType.PING); // Which premade messages should receive Spigot
    private static final List<SocketMessageType> BUNGEE_RESPOND_TYPES = List.of(SocketMessageType.PONG, SocketMessageType.REGISTER, SocketMessageType.UNREGISTER); // Which premade messages should receive Bungee

    public String serverName(){
        return switch (this){
            case SPIGOT -> ru.shk.configapi.ConfigAPI.getServerName();
            case BUNGEE -> "BungeeCord";
        };
    }

    public SocketManager defaultSocketManager(){
        return switch (this){
            case SPIGOT -> ru.shk.commons.Commons.getInstance().getSocketManager();
            case BUNGEE -> ru.shk.commonsbungee.Commons.getInstance().getSocketManager();
        };
    }

    private boolean isRespondCase(SocketMessageType type){
        return switch (this){
            case SPIGOT -> SPIGOT_RESPOND_TYPES.contains(type);
            case BUNGEE -> BUNGEE_RESPOND_TYPES.contains(type);
        };
    }

    public boolean respond(SocketManager manager, Socket currentSocket, SocketMessageType type, String server, DataInputStream in){
        if(!isRespondCase(type) || !manager.getSocketThread().equals(defaultSocketManager().getSocketThread())) return false;
        try {
            switch (SocketManager.serverType){
                case BUNGEE -> {
                    switch (type){
                        case REGISTER -> {
                            int port = in.readInt();
                            defaultSocketManager().getLogConsumer().accept("&a  Registering server "+server+" at "+port);
                            defaultSocketManager().getBackendServers().removeIf(socketServerInfo -> socketServerInfo.getName().equals(server) || socketServerInfo.getAddress().getPort()==port);
                            defaultSocketManager().getBackendServers().add(new SocketServerInfo(new InetSocketAddress(((InetSocketAddress) currentSocket.getRemoteSocketAddress()).getAddress().getHostAddress(), port), server));
                            manager.sendData(server, SocketMessageType.PING, List.of());
                        }
                        case UNREGISTER -> defaultSocketManager().getBackendServers().removeIf(socketServerInfo -> socketServerInfo.getName().equals(server));
                        case PONG -> defaultSocketManager().getLogConsumer().accept("&a  Received pong from "+server);
                        default -> {
                            return false;
                        }
                    }
                }
                case SPIGOT -> {
                    switch (type){
                        case PING -> {
                            defaultSocketManager().getLogConsumer().accept("&a  Received ping from Bungee");
                            defaultSocketManager().sendToBungee(SocketMessageType.PONG, List.of());
                        }
                        default -> {
                            return false;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Consumer<String> log = defaultSocketManager().getLogConsumer();
            log.accept(" > "+t.getClass().getSimpleName()+": "+t.getMessage());
            for (StackTraceElement st : t.getStackTrace()) {
                log.accept("  "+st.toString());
            }
        }
        return true;
    }

}
