package ru.shk.commons.utils.gui;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.shk.commons.utils.Logger;
import ru.shk.commons.utils.Plugin;
import ru.shk.commons.utils.runnables.Schedule;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class GUIManager implements Plugin {
    @Getter@Accessors(fluent = true)
    private static GUIManager instance;
    private final HashMap<UUID, GUI> openGUIs = new HashMap<>();
    @Getter@Setter
    private boolean debugClose = false;

    @Override
    public void load() {
        instance = this;
    }

    @Override
    public void enable() {
        Runnable tickTask = () -> {
            new ArrayList<>(openGUIs.values()).parallelStream().forEach(gui -> {
                try {
                    gui.doTick();
                } catch (Throwable t){
                    t.printStackTrace();
                }
            });
        };
        Schedule.syncRepeating(tickTask, Duration.ofMillis(50), Duration.ofMillis(50));
        
//        Protocolize.listenerProvider().registerListener(new AbstractPacketListener<>(InventoryClick.class, Direction.UPSTREAM, 0) {
//            @Override
//            public void packetReceive(PacketReceiveEvent<InventoryClick> e) {
//
//            }
//
//            @Override
//            public void packetSend(PacketSendEvent<InventoryClick> e) {
//                if (!e.player().registeredInventories().isEmpty() && e.packet().slot() == -999) {
//                    e.cancelled(true);
//                }
//            }
//        });
    }

    public GUI customGUI(Object inventory) {
        synchronized (openGUIs) {
            for (GUI gui : openGUIs.values()) {
                if(!gui.isThisInventory(inventory)) continue;
                return gui;
            }
        }
        return null;
    }

    public void add(UUID player, GUI gui) {
        synchronized (openGUIs) {
            openGUIs.put(player, gui);
            if(debugClose) Logger.info("Added "+gui.getClass().getSimpleName()+" to "+player.toString());
        }
    }

    public void removeGUIsOf(UUID uuid) {
        GUI g;
        synchronized (openGUIs) {
            g = openGUIs.get(uuid);
        }
        if (g==null) return;
        g.onClose();
        if (debugClose){
            Logger.info("Removed guis of "+uuid.toString());
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            for (StackTraceElement stackTraceElement : stackTraceElements) Logger.info(" "+stackTraceElement.toString());
        }
    }

    public void removeGUI(UUID uuid, GUI gui) {
        synchronized (openGUIs) {
            openGUIs.remove(uuid, gui);
        }
        if (debugClose){
            Logger.info("Removed gui "+gui.getClass().getSimpleName()+" of "+uuid.toString());
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            for (StackTraceElement stackTraceElement : stackTraceElements) Logger.info(" "+stackTraceElement.toString());
        }
    }

    public void onPluginDisabled(Object plugin){
        synchronized (openGUIs){
            new ArrayList<>(openGUIs.values()).forEach((gui) -> {
                if (gui.plugin().equals(plugin)) {
                    gui.close();
                }
            });
        }
    }

    @Override
    public void disable() {
        synchronized (openGUIs) {
            new ArrayList<>(openGUIs.values()).forEach(GUI::close);
        }
    }
}
