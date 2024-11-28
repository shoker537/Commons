package ru.shk.commons.utils.gui;

import lombok.Getter;
import lombok.experimental.Accessors;
import ru.shk.commons.ServerType;
import ru.shk.commons.utils.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class GUIManager implements Plugin {
    @Getter@Accessors(fluent = true)
    private static GUIManager instance;
    private final HashMap<UUID, GUI> openGUIs = new HashMap<>();

    @Override
    public void load() {
        instance = this;
    }

    @Override
    public void enable() {
        Runnable tickTask = () -> {
            openGUIs.values().parallelStream().forEach(gui -> {
                try {
                    gui.doTick();
                } catch (Throwable t){
                    t.printStackTrace();
                }
            });
        };
        if(ServerType.get()==ServerType.VELOCITY) {
            ru.shk.velocity.commons.Commons.getInstance().repeat(tickTask, Duration.ofMillis(50), Duration.ofMillis(50));
        } else if (ServerType.get()==ServerType.SPIGOT) {
            ru.shk.commons.Commons.getInstance().syncRepeating(tickTask, 1,1);
        }
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
        }
    }

    public void removeGUIsOf(UUID uuid) {
        synchronized (openGUIs) {
            openGUIs.remove(uuid);
        }
    }

    public void removeGUI(UUID uuid, GUI gui) {
        synchronized (openGUIs) {
            openGUIs.remove(uuid, gui);
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
