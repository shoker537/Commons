package ru.shk.velocity.commons.utils;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lombok.Getter;
import lombok.experimental.Accessors;
import ru.shk.velocity.commons.Commons;


public class PluginMessage {
    private final String channel;
    @Getter@Accessors(fluent = true)private final ByteArrayDataOutput o = ByteStreams.newDataOutput();

    public PluginMessage(String channel) {
        this.channel = channel;
    }

    public PluginMessage writeUTF(String text){
        o.writeUTF(text);
        return this;
    }
    public PluginMessage writeInt(int i){
        o.writeInt(i);
        return this;
    }
    public PluginMessage writeShort(short s){
        o.writeShort(s);
        return this;
    }
    public PluginMessage writeDouble(double d){
        o.writeDouble(d);
        return this;
    }

    public MinecraftChannelIdentifier identifier(){
        return MinecraftChannelIdentifier.from(channel);
    }

    public void send(ChannelMessageSink server){
        server.sendPluginMessage(identifier(), o.toByteArray());
    }
    public void send(String server){send(Commons.getInstance().proxy().getServer(server).get());}
}
