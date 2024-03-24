package ru.shk.guilibbungee;

import dev.simplix.protocolize.api.Direction;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.chat.ChatElement;
import dev.simplix.protocolize.api.inventory.Inventory;
import dev.simplix.protocolize.api.listener.AbstractPacketListener;
import dev.simplix.protocolize.api.listener.PacketReceiveEvent;
import dev.simplix.protocolize.api.listener.PacketSendEvent;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import dev.simplix.protocolize.data.ItemType;
import dev.simplix.protocolize.data.inventory.InventoryType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import ru.shk.commonsbungee.ItemStackBuilder;
import ru.shk.guilib.protocolize.packet.RenameItemPacket;

import java.util.List;
import java.util.function.Consumer;

public class TextInputGUI extends Inventory {
    private String text = "";
    private final AbstractPacketListener<RenameItemPacket> listener;
    private final ProxiedPlayer player;

    public TextInputGUI(ProxiedPlayer player, String title, String originalName, List<String> description, Consumer<String> result) {
        this(player, title, false, originalName, description, result);
    }

    public TextInputGUI(ProxiedPlayer player, String title, boolean json, String originalName, List<String> description, Consumer<String> result) {
        this(player, title, json, new ItemStackBuilder(ItemType.PAPER), originalName, description, result);
    }

    public TextInputGUI(ProxiedPlayer player, String title, boolean json, ItemStackBuilder item, String originalName, List<String> description, Consumer<String> result) {
        super(InventoryType.ANVIL);
        this.player = player;
        if(json) title(ChatElement.ofJson(title)); else title(ChatElement.ofLegacyText(title));
        item(0, item.displayName(originalName).lore(description).build());
        onClick(click -> {
            click.cancelled(true);
            if(click.slot()==2){
                if(text.isEmpty()) return;
                close(player);
                result.accept(text);
            }
        });
        GUILib.getTextInputGUIS().add(this);
        open(player);
        listener = new AbstractPacketListener<>(RenameItemPacket.class, Direction.UPSTREAM, 0) {
            @Override
            public void packetReceive(PacketReceiveEvent<RenameItemPacket> event) {
                if(!event.player().uniqueId().equals(player.getUniqueId())) return;
                text = event.packet().itemName();
                event.cancelled(true);
            }

            @Override
            public void packetSend(PacketSendEvent<RenameItemPacket> event) {

            }
        };
        onClose(inventoryClose -> {
            ProxiedPlayer p = ProxyServer.getInstance().getPlayer(inventoryClose.player().uniqueId());
            if(p!=null) closed(p);
        });
    }

    public boolean closed(ProxiedPlayer p){
        if(!p.getUniqueId().equals(player.getUniqueId())) return false;
        try {
            Protocolize.listenerProvider().unregisterListener(listener);
        } catch (Throwable t){}
        return true;
    }

    private void open(ProxiedPlayer p){
        ProtocolizePlayer player = Protocolize.playerProvider().player(p.getUniqueId());
        player.closeInventory();
        player.openInventory(this);
        Protocolize.listenerProvider().registerListener(listener);
    }

    private static void close(ProxiedPlayer p){
        ProtocolizePlayer player = Protocolize.playerProvider().player(p.getUniqueId());
        player.closeInventory();
    }
}
