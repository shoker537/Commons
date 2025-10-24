package ru.shk.guilib.protocolize.packet;

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
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_20_1, MINECRAFT_1_20_4, 0x23),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_20_5, MINECRAFT_1_21_1, 0x2A),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_21_2, MINECRAFT_1_21_3, 0x2C),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_21_4, MINECRAFT_1_21_5, 0x2E),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_21_6, Integer.MAX_VALUE, 0x2E)
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
