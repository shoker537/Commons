package ru.shk.commons.sockets;

import lombok.Getter;
import ru.shk.commons.sockets.low.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

public class SocketThread extends Thread {
    @Getter private ServerSocket socket;
    @Getter private final ThreadPoolExecutor sendQueue = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    private final SocketManager manager;
    @Getter private boolean isServerStarted = false;
    private final int port;

    public SocketThread(int port, SocketManager manager){
        this.manager = manager;
        this.port = port;
        try {
            socket = new ServerSocket(port);
            socket.setSoTimeout(0);
            socket.setReuseAddress(true);
            isServerStarted = true;
            manager.getLogConsumer().accept("&aSocket server started at "+port+"!");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    public void close(){
        try {
            socket.close();
        } catch (IOException e) {}
        interrupt();
    }

    public void sendData(SocketServerInfo adress, SocketMessageType type, String channel, List<Object> data){
        sendQueue.submit(() -> sendDataSync(adress.getAddress(), type, channel, data));
    }

    private void sendDataSync(InetSocketAddress adress, SocketMessageType type, String channel, List<Object> data){
        Consumer<String> log = manager.getLogConsumer();
        try {
            log.accept("&e Sending new message >>>");
            log.accept("&e  Type: "+type.name());
            log.accept("&e  To: "+adress.getPort());
            Socket s = new Socket(adress.getHostName(), adress.getPort());
            s.setReuseAddress(true);
            int t =  0;
            while (!s.isConnected()){
                t++;
                Thread.sleep(100);
                if(t==10) {
                    log.accept("&c Connection timed out");
                    return;
                }
            }
            try (s; DataOutputStream out = new DataOutputStream(s.getOutputStream())) {
                s.setSoTimeout(5000);
                out.writeUTF(type==SocketMessageType.SEVERE?channel:type.name());
                out.writeUTF(manager.getCurrentServerName());
                for (Object o : data) {
                    if (o instanceof String i) {
                        out.writeUTF(i);
                    } else if (o instanceof Byte i) {
                        out.writeByte(i);
                    } else if (o instanceof Integer i) {
                        out.writeInt(i);
                    } else if (o instanceof Double i) {
                        out.writeDouble(i);
                    } else if (o instanceof Float i) {
                        out.writeFloat(i);
                    } else if (o instanceof Long i) {
                        out.writeLong(i);
                    } else if (o instanceof Short i) {
                        out.writeShort(i);
                    } else if (o instanceof Character i) {
                        out.writeChar(i);
                    } else if (o instanceof Boolean b) {
                        out.writeBoolean(b);
                    } else {
                        throw new NotAcceptedObjectTypeException();
                    }
                }
                out.writeUTF(SocketMessageType.END_MESSAGE);
                out.flush();
                log.accept("&a Sent successfully!");
            }
        } catch (Throwable t) {
            log.accept("&c  Ended with an error");
            log.accept(" > "+t.getClass().getSimpleName()+": "+t.getMessage());
            for (StackTraceElement st : t.getStackTrace()) {
                log.accept("  "+st.toString());
            }
        }
    }

    @Override
    public void run() {
        if(!isServerStarted){
            manager.getLogConsumer().accept("&cUnable to run SocketThread, socket server haven't started!");
            return;
        }
        if(SocketManager.serverType==ServerType.SPIGOT){
            manager.getLogConsumer().accept("&eSending REGISTER to BungeeCord...");
            try {
                manager.sendToBungee(SocketMessageType.REGISTER, List.of(socket.getLocalPort()));
            } catch (Throwable t){
                Consumer<String> log = manager.getLogConsumer();
                log.accept(" > "+t.getClass().getSimpleName()+": "+t.getMessage());
                for (StackTraceElement st : t.getStackTrace()) {
                    log.accept("  "+st.toString());
                }
            }
        }
        while (true){
            int t =  0;
            while (socket.isClosed()){
                t++;
                try {
                    socket = new ServerSocket(port);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(t==10) {
                    manager.getLogConsumer().accept("&c Server closed and reconnection timed out!");
                    return;
                }
            }
            try (Socket s = socket.accept()) {
                manager.getLogConsumer().accept("&e <<< New message received");
                DataInputStream in = new DataInputStream(s.getInputStream());
                String channel = in.readUTF();
                String server = in.readUTF();
                SocketMessageType type = null;
                for (SocketMessageType value : SocketMessageType.values()) {
                    if(value.name().equals(channel)) type = value;
                }
                if(type==null) type = SocketMessageType.SEVERE;
                if(manager.getSocketServerInfo(server).isEmpty() && type!=SocketMessageType.REGISTER) {
                    manager.getLogConsumer().accept("&c Received message from unregistered server '"+server+"'! Skipped.");
                    continue;
                }
                manager.getLogConsumer().accept("&eServer: "+server);
                manager.getLogConsumer().accept("&eChannel: "+channel);
                manager.getLogConsumer().accept("&eType: "+type.name());
                if (type != SocketMessageType.SEVERE && SocketManager.serverType.respond(manager, s, type, server, in)) continue;
                SocketMessageType finalType = type;
                manager.getSocketMessageListeners().stream().filter(listener -> listener.getChannel() == null || (finalType==SocketMessageType.SEVERE && listener.getChannel().equals(channel))).forEach(socketMessageListener -> {
                    try {
                        socketMessageListener.onMessage(manager, finalType, channel, server, in);
                    } catch (Throwable t1) {
                        Consumer<String> log = manager.getLogConsumer();
                        log.accept(" > "+t1.getClass().getSimpleName()+": "+t1.getMessage());
                        for (StackTraceElement st : t1.getStackTrace()) {
                            log.accept("  "+st.toString());
                        }
                    }
                });
            } catch (Throwable t1) {
                Consumer<String> log = manager.getLogConsumer();
                log.accept(" > "+t1.getClass().getSimpleName()+": "+t1.getMessage());
                for (StackTraceElement st : t1.getStackTrace()) {
                    log.accept("  "+st.toString());
                }
            }
        }
    }
}
