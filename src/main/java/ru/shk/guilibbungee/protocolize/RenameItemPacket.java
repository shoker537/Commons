package ru.shk.guilibbungee.protocolize;

import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.mapping.AbstractProtocolMapping;
import dev.simplix.protocolize.api.mapping.ProtocolIdMapping;
import dev.simplix.protocolize.api.packet.AbstractPacket;
import dev.simplix.protocolize.api.util.ProtocolUtil;
import io.netty.buffer.ByteBuf;
import lombok.*;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static dev.simplix.protocolize.api.util.ProtocolVersions.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(fluent = true)
public class RenameItemPacket extends AbstractPacket {
    public static final List<ProtocolIdMapping> MAPPINGS = List.of(
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19, MINECRAFT_1_19, 0x22),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19_1, MINECRAFT_LATEST, 0x23)
    );

    private String itemName;
    @Override
    public void read(ByteBuf buf, PacketDirection packetDirection, int i) {
        itemName = ProtocolUtil.readString(buf);
    }

    @Override
    public void write(ByteBuf buf, PacketDirection packetDirection, int i) {
        buf.writeCharSequence(itemName, StandardCharsets.UTF_8);
    }
}
