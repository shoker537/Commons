package ru.shk.velocity.commons.gui;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import lombok.Getter;
import ru.shk.commons.utils.Plugin;
import ru.shk.velocity.commons.Commons;

import java.util.ArrayList;
import java.util.List;

public class GUILib implements Plugin {
    @Getter
    private static final List<TextInputGUI> textInputGUIS = new ArrayList<>();
    @Override
    public void load() {

    }

    @Override
    public void enable() {
        Commons.getInstance().proxy().getEventManager().register(Commons.getInstance(), this);
    }

    @Override
    public void disable() {
        Commons.getInstance().proxy().getEventManager().unregisterListener(Commons.getInstance(), this);
    }

    @Subscribe
    public void onQuit(DisconnectEvent e){
        List<TextInputGUI> toRemove = new ArrayList<>();
        for (TextInputGUI gui : textInputGUIS) {
            if(gui.closed(e.getPlayer())) toRemove.add(gui);
        }
        textInputGUIS.removeAll(toRemove);
    }
    @Subscribe
    public void serverSwitch(ServerConnectedEvent e){
        List<TextInputGUI> toRemove = new ArrayList<>();
        for (TextInputGUI gui : textInputGUIS) {
            if(gui.closed(e.getPlayer())) toRemove.add(gui);
        }
        textInputGUIS.removeAll(toRemove);
    }
}
