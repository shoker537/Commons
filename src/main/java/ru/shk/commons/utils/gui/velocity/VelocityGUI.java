package ru.shk.commons.utils.gui.velocity;

import com.google.common.collect.Lists;
import com.velocitypowered.api.proxy.Player;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.chat.ChatElement;
import dev.simplix.protocolize.api.inventory.Inventory;
import dev.simplix.protocolize.api.item.BaseItemStack;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import dev.simplix.protocolize.data.inventory.InventoryType;
import dev.simplix.protocolize.data.packets.WindowItems;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import ru.shk.commons.utils.Logger;
import ru.shk.commons.utils.gui.ClickEvent;
import ru.shk.commons.utils.gui.GUI;
import ru.shk.commons.utils.gui.Item;
import ru.shk.commons.utils.gui.UniversalClick;
import ru.shk.commons.utils.items.ItemStackBuilder;
import ru.shk.commons.utils.items.velocity.VelocityItemStack;
import ru.shk.velocity.commons.Commons;
import ru.shk.velocity.commons.utils.PluginMessage;
import dev.simplix.protocolize.api.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

public class VelocityGUI extends GUI<VelocityGUI> {
    private static final ThreadPoolExecutor syncExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    private Inventory inventory;
    private int windowId = -1;
    private int state = 0;

    public VelocityGUI(Object plugin, GUIType type, Player player, Component title) {
        super(plugin, type, player, title);
    }

    @Override
    public void open() {
        super.open();
        if(inventory==null) {
            inventory = new Inventory(typeAsProtocolize(type(), lines()));
            inventory.title(ChatElement.of(title()));
            inventory.onClick(click -> {
                if(click.clickedItem()==null) return;
                ClickType type = switch (click.clickType()) {
                    case LEFT_CLICK, SHIFT_LEFT_CLICK -> ClickType.LEFT;
                    case RIGHT_CLICK, SHIFT_RIGHT_CLICK -> ClickType.RIGHT;
                    case CREATIVE_MIDDLE_CLICK -> ClickType.MIDDLE;
                    default -> null;
                };
                if(type==null) return;
                onClick(new UniversalClick(click.slot(), new VelocityItemStack(click.clickedItem()), type, click.clickType().name().startsWith("SHIFT_")));
            });
            inventory.onClose(inventoryClose -> onClose());
        }
        ProtocolizePlayer player = Protocolize.playerProvider().player(player().getUniqueId());
        if(player==null) return;
        player.openInventory(inventory);
        windowId = getInvId();
    }

    @Override
    public VelocityGUI item(int slot, @Nullable ItemStackBuilder stack) {
        if (stack==null) {
            inventory.removeItem(slot);
            if(isOpen()) update();
            return this;
        }
        return super.item(slot, stack);
    }

    @Override
    public void title(Component title) {
        inventory.title(ChatElement.of(title));
        reopen();
    }

    @Override
    public void sync(Runnable r) {
        syncExecutor.submit(r);
    }

    @Override
    public void async(Runnable r) {
        Commons.getInstance().async(r);
    }

    public VelocityGUI item(int slot, ItemStack stack) {
        return item(slot, new VelocityItemStack(stack));
    }
    public VelocityGUI item(int slot, @NonNull ItemStack stack, Consumer<ClickEvent> onClick) {
        return item(slot, new VelocityItemStack(stack), onClick);
    }

    public int getInvId(){
        int windowId = -1;
        ProtocolizePlayer player = Protocolize.playerProvider().player(player().getUniqueId());
        for (Integer id : player.registeredInventories().keySet()) {
            if(player.registeredInventories().get(id).equals(this)) {
                windowId = id;
                break;
            }
        }
        return windowId;
    }

    public Player player(){
        return (Player) super.player();
    }

    @Override
    public void close() {
        ProtocolizePlayer player = Protocolize.playerProvider().player(player().getUniqueId());
        if(player==null) return;
        player.closeInventory();
    }

    @Override
    public void updateLocalInv() {
        player().getCurrentServer().ifPresent(server -> Commons.getInstance().async(() -> new PluginMessage("commons:updateinv").writeUTF(player().getUniqueId().toString()).send(server)));
    }

    @Override
    public void refillInv() {
        state++;
        for (int i = 0; i < items().length; i++) {
            Item item = items()[i];
            inventory.item(i, (ItemStack) item.stack().build());
        }
        ProtocolizePlayer player = Protocolize.playerProvider().player(player().getUniqueId());
        if(player==null) return;
        if(windowId==-1) {
            Logger.info("WindowID not found for GUI of player "+player().getUsername());
            return;
        }
        List<BaseItemStack> items = new ArrayList<>(Lists.newArrayList(inventory.itemsIndexed(player.protocolVersion())));
        Commons.getInstance().async(() -> player.sendPacket(new WindowItems((short) windowId, items, state)));
    }

    @Override
    public UUID playerUUID() {
        return player().getUniqueId();
    }

    private static InventoryType typeAsProtocolize(GUIType type, int rows) {
        return switch (type) {
            case ANVIL -> InventoryType.ANVIL;
            case CHEST -> InventoryType.chestInventoryWithRows(rows);
        };
    }

    @Override
    public boolean isThisInventory(Object inventory) {
        return inventory.equals(this.inventory);
    }

}
