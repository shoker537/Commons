package ru.shk.guilibbungee;

import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.inventory.Inventory;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import dev.simplix.protocolize.data.ItemType;
import dev.simplix.protocolize.data.inventory.InventoryType;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import ru.shk.commonsbungee.ItemStackBuilder;

import java.util.List;
import java.util.function.Consumer;

public class TextInputGUI extends Inventory {
    public TextInputGUI(ProxiedPlayer player, String title, String originalName, List<String> description, Consumer<String> result) {
        super(InventoryType.ANVIL);
        title(title).item(0, new ItemStackBuilder(ItemType.PAPER).displayName(originalName).lore(description).build()).item(2, new ItemStackBuilder(ItemType.PAPER).displayName("").build());
        onClick(click -> {
            click.cancelled(true);
            if(click.slot()==2){
                String n = item(0).displayName(true);
                if(n.length()==0) return;
                result.accept(n);
            }
        });
        open(player);
    }

    private void open(ProxiedPlayer p){
        ProtocolizePlayer player = Protocolize.playerProvider().player(p.getUniqueId());
        player.closeInventory();
        player.openInventory(this);
    }
}
