package ru.shk.velocity.commons;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import de.simonsator.partyandfriends.velocity.api.party.PartyManager;
import de.simonsator.partyandfriends.velocity.api.party.PlayerParty;
import land.shield.playerapi.CachedPlayer;
import ru.shk.commons.utils.GlobalParty;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PAFManager {

    public PAFManager(Commons commons){
        commons.proxy().getChannelRegistrar().register(MinecraftChannelIdentifier.from("commons:paf"));
    }

    @Nullable
    public GlobalParty getParty(UUID player){
        PlayerParty party = PartyManager.getInstance().getParty(player);
        if(party==null || party.getPlayers().isEmpty()) return null;
        List<UUID> p = new ArrayList<>();
        p.add(party.getLeader().getUniqueId());
        party.getPlayers().forEach(onlinePAFPlayer -> {
            if(!p.contains(onlinePAFPlayer.getUniqueId())) p.add(onlinePAFPlayer.getUniqueId());
        });
        return new GlobalParty(p.stream().map(CachedPlayer::of).toList(), CachedPlayer.of(party.getLeader().getUniqueId()));
    }
}
