package ru.shk.commons.utils.gui.velocity;

import com.velocitypowered.api.proxy.Player;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.chat.ChatElement;
import dev.simplix.protocolize.api.inventory.Inventory;
import dev.simplix.protocolize.api.item.ItemStack;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import dev.simplix.protocolize.data.ItemType;
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

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class VelocityGUI extends GUI<VelocityGUI> {
    private static final ThreadPoolExecutor syncExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    private Inventory inventory;
    private final AtomicInteger windowId = new AtomicInteger(-1);
    private int state = 0;
    private final AtomicBoolean needsUpdate = new AtomicBoolean();

    public VelocityGUI(Object plugin, GUIType type, Player player, Component title) {
        super(plugin, type, player, title);
    }

    @Override
    public void open() {
        if(inventory==null) {
            inventory = new Inventory(typeAsProtocolize(type(), lines()));
            inventory.title(ChatElement.of(title()));
            inventory.onClick(click -> {
                click.cancelled(true);
                updateLocalInv();
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
        refillInv();
        player.openInventory(inventory);
        for (int i = 0; i < 5; i++) {
            windowId.set(getInvId());
            if(windowId.get()!=-1) break;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {}
        }
        super.open();
        update();
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    @Override
    public VelocityGUI item(int slot, @Nullable ItemStackBuilder stack) {
        if (stack==null) {
            clear(slot);
            return this;
        }
        return super.item(slot, stack);
    }

    @Override
    public VelocityGUI title(Component title) {
        if (inventory!=null){
            inventory.title(ChatElement.of(title));
            reopen();
        }
        super.title(title);
        return this;
    }

    @Override
    public void sync(Runnable r) {
        syncExecutor.submit(r);
    }

    public VelocityGUI item(int slot, @NonNull ItemStack stack) {
        return item(slot, new VelocityItemStack(stack).clone());
    }
    public VelocityGUI item(int slot, @NonNull ItemStack stack, Consumer<ClickEvent> onClick) {
        return item(slot, new VelocityItemStack(stack).clone(), onClick);
    }
    public VelocityGUI item(int slot, @NonNull ItemStack stack, Consumer<ClickEvent> onClick, boolean runAsync) {
        return item(slot, new VelocityItemStack(stack).clone(), onClick, runAsync);
    }

    public int getInvId(){
        int windowId = -1;
        ProtocolizePlayer player = Protocolize.playerProvider().player(player().getUniqueId());
        for (Integer id : player.registeredInventories().keySet()) {
            if(Objects.equals(player.registeredInventories().get(id), inventory)) {
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
        needsUpdate.set(true);
    }

    @Override
    public void doTick() {
        super.doTick();
        doRefillInv();
    }

    public void doRefillInv(){
        if(!needsUpdate.get()) return;
        needsUpdate.set(false);
        state++;
        int max = type()==GUIType.CHEST?lines()*9:type().maxSlots();
        for (int i = 0; i < max; i++) {
            Item item = items().get(i);
            if(item==null || item.stack().type().name().equals("AIR")) {
                inventory.item(i, new ItemStack(ItemType.AIR));
            } else {
                inventory.item(i, (ItemStack) item.stack().build());
            }
        }
        ProtocolizePlayer player = Protocolize.playerProvider().player(player().getUniqueId());
        if(player==null) return;
        if(windowId.get()==-1) {
            if(isOpen()) Logger.info("WindowID not found for GUI of player "+player().getUsername());
            return;
        }
//        List<BaseItemStack> items = new ArrayList<>();
//        var all = inventory.itemsIndexed(player.protocolVersion());
//        Logger.warning("indexed: "+all.size()+", lines: "+lines()+", type: "+type().name()+", maxslots: "+type().maxSlots()+", protocolizeType: "+typeAsProtocolize(GUIType.CHEST, lines()).name());
//        for (BaseItemStack baseItemStack : all) {
//            if (items.size()==54) {
//                break;
//            }
//            items.add(new ItemStack(baseItemStack.itemType()));
//        }
        player.sendPacket(new WindowItems(windowId.get(), new ArrayList<>(inventory.itemsIndexed(player.protocolVersion())), state));
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
