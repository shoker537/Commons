package ru.shk.commons.utils;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import ru.shk.commons.Commons;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

public class PAFManager {
    private final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
    private final List<PartyWaiter> waitingForParty = new ArrayList<>();

    public PAFManager(Commons commons){
        commons.getServer().getMessenger().registerIncomingPluginChannel(commons, "commons:paf", (s, player, bytes) -> commons.async(() -> {
            ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
            List<UUID> party = new ArrayList<>();
            while (true){
                try {
                    String a = in.readUTF();
                    if(a.equals("not-in-party")) {
                        receivedParty(player.getUniqueId(), null);
                        return;
                    }
                    party.add(UUID.fromString(a));
                } catch (Exception e){
                    break;
                }
            }
            receivedParty(player.getUniqueId(), party);
        }));
    }

    private void receivedParty(UUID uuid, @Nullable List<UUID> party){
        waitingForParty.stream().filter(partyWaiter -> partyWaiter.uuid.equals(uuid)).forEach(partyWaiter -> partyWaiter.whenReceived.accept(party));
    }

    public void requestParty(Player p, Consumer<List<UUID>> whenReceived){
        waitingForParty.add(new PartyWaiter(p.getUniqueId(), whenReceived));
        sendPartyRequest(p);
    }

    private void sendPartyRequest(Player p){
        ByteArrayDataOutput o = ByteStreams.newDataOutput();
        o.writeUTF("CPAF");
        o.writeUTF("GetParty");
        p.sendPluginMessage(Commons.getInstance(), "BungeeCord", o.toByteArray());
    }

    private class PartyWaiter {
        private final UUID uuid;
        private final Consumer<List<UUID>> whenReceived;

        public PartyWaiter(UUID uuid, Consumer<List<UUID>> whenReceived) {
            this.uuid = uuid;
            this.whenReceived = whenReceived;
        }
    }
}
