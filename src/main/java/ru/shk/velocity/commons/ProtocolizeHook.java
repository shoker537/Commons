package ru.shk.velocity.commons;

import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.Protocol;
import dev.simplix.protocolize.api.Protocolize;
import ru.shk.commons.utils.gui.protocolize.packet.RenameItemPacket;

public class ProtocolizeHook {
    public static void register(){
        Protocolize.protocolRegistration().registerPacket(RenameItemPacket.MAPPINGS, Protocol.PLAY, PacketDirection.SERVERBOUND, RenameItemPacket.class);
    }
}
