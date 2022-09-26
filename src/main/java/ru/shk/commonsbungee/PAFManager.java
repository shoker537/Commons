package ru.shk.commonsbungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.simonsator.partyandfriends.api.party.PartyManager;
import de.simonsator.partyandfriends.api.party.PlayerParty;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PAFManager {

    public PAFManager(Commons commons){
        commons.getProxy().registerChannel("commons:paf");
    }

    public void acceptPluginMessage(ProxiedPlayer player, ByteArrayDataInput in){
        switch (in.readUTF()){
            case "GetParty" -> {
                List<UUID> party = getParty(player.getUniqueId());
                if(party==null){
                    sendPartyResponse(player, List.of("not-in-party"));
                    return;
                }
                sendPartyResponse(player, party.stream().map(UUID::toString).toList());
            }
        }
    }

    private void sendPartyResponse(ProxiedPlayer player, List<String> data){
        ByteArrayDataOutput o = ByteStreams.newDataOutput();
        o.writeUTF("PartyInfo");
        data.forEach(o::writeUTF);
        player.getServer().sendData("commons:paf", o.toByteArray());
    }

    @Nullable
    public List<UUID> getParty(UUID player){
        PlayerParty party = PartyManager.getInstance().getParty(player);
        if(party==null || party.getPlayers().size()==0) return null;
        List<UUID> p = new ArrayList<>();
        p.add(party.getLeader().getUniqueId());
        party.getPlayers().forEach(onlinePAFPlayer -> {
            if(!p.contains(onlinePAFPlayer.getUniqueId())) p.add(onlinePAFPlayer.getUniqueId());
        });
        return p;
    }
}
