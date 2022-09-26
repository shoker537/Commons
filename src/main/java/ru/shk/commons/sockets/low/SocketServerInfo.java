package ru.shk.commons.sockets.low;

import lombok.Getter;

import java.net.InetSocketAddress;

@Getter
public class SocketServerInfo {
    private final InetSocketAddress address;
    private final String name;

    public SocketServerInfo(InetSocketAddress address, String name) {
        this.address = address;
        this.name = name;
    }
}
