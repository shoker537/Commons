package ru.shk.guilibbungee;

import dev.simplix.protocolize.api.Direction;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.inventory.Inventory;
import dev.simplix.protocolize.api.listener.AbstractPacketListener;
import dev.simplix.protocolize.api.listener.PacketReceiveEvent;
import dev.simplix.protocolize.api.listener.PacketSendEvent;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import dev.simplix.protocolize.api.util.ProtocolUtil;
import dev.simplix.protocolize.data.ItemType;
import dev.simplix.protocolize.data.inventory.InventoryType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import ru.shk.commonsbungee.ItemStackBuilder;
import ru.shk.guilibbungee.protocolize.RenameItemPacket;

import java.util.List;
import java.util.function.Consumer;

public class TextInputGUI extends Inventory {
    private String text = "";
    private final AbstractPacketListener<RenameItemPacket> listener;

    public TextInputGUI(ProxiedPlayer player, String title, String originalName, List<String> description, Consumer<String> result) {
        super(InventoryType.ANVIL);
        title(title).item(0, new ItemStackBuilder(ItemType.PAPER).displayName(originalName).lore(description).build());
        onClick(click -> {
            click.cancelled(true);
            if(click.slot()==2){
                if(text.length()==0) return;
                close(player);
                result.accept(text);
            }
        });
        open(player);
        listener = new AbstractPacketListener<>(RenameItemPacket.class, Direction.UPSTREAM, 0) {
            @Override
            public void packetReceive(PacketReceiveEvent<RenameItemPacket> event) {
                text = event.packet().itemName();
            }

            @Override
            public void packetSend(PacketSendEvent<RenameItemPacket> event) {

            }
        };
        Protocolize.listenerProvider().registerListener(listener);
        onClose(inventoryClose -> Protocolize.listenerProvider().unregisterListener(listener));
    }

    private void open(ProxiedPlayer p){
        ProtocolizePlayer player = Protocolize.playerProvider().player(p.getUniqueId());
        player.closeInventory();
        player.openInventory(this);
    }

    private static void close(ProxiedPlayer p){
        ProtocolizePlayer player = Protocolize.playerProvider().player(p.getUniqueId());
        player.closeInventory();
    }
}
