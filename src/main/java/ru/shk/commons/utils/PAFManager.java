package ru.shk.commons.utils;

import land.shield.playerapi.CachedPlayer;
import ru.shk.commons.Commons;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PAFManager {
//    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 5, 30L, TimeUnit.SECONDS, new SynchronousQueue<>());
//    private final List<PartyWaiter> waitingForParty = new ArrayList<>();

    public PAFManager(Commons commons){
//        commons.getServer().getMessenger().registerIncomingPluginChannel(commons, "commons:paf", (s, player, bytes) -> commons.async(() -> {
//            ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
//            List<UUID> party = new ArrayList<>();
//            while (true){
//                try {
//                    String a = in.readUTF();
//                    if(a.equals("not-in-party")) {
//                        receivedParty(player.getUniqueId(), null);
//                        return;
//                    }
//                    party.add(UUID.fromString(a));
//                } catch (Exception e){
//                    break;
//                }
//            }
//            receivedParty(player.getUniqueId(), party);
//        }));
    }

//    private void receivedParty(UUID uuid, @Nullable List<UUID> party){
//        waitingForParty.stream().filter(partyWaiter -> partyWaiter.uuid.equals(uuid)).forEach(partyWaiter -> partyWaiter.whenReceived.accept(party));
//    }
//
//    public void requestParty(Player p, Consumer<List<UUID>> whenReceived){
//        waitingForParty.add(new PartyWaiter(p.getUniqueId(), whenReceived));
//        sendPartyRequest(p);
//    }

    @Nullable
    public GlobalParty getPartyFromDatabase(UUID player){
        List<CachedPlayer> players = new ArrayList<>();
        CachedPlayer owner = null;
        try (ResultSet rs = Commons.getInstance().getMysql().Query(
                "SELECT (SELECT player_uuid FROM fr_players players WHERE players.player_id=party.player_member_id LIMIT 1) AS member_uuid, (SELECT player_uuid FROM fr_players players WHERE players.player_id=party.leader_id LIMIT 1) AS owner_uuid FROM fr_party party WHERE leader_id = (SELECT leader_id FROM fr_party WHERE player_member_id=(SELECT player_id FROM fr_players WHERE player_uuid = '"+player+"' LIMIT 1) LIMIT 1)"
        )) {
            while (rs.next()){
                players.add(CachedPlayer.of(UUID.fromString(rs.getString(1))));
                if(owner==null) owner = CachedPlayer.of(UUID.fromString(rs.getString(2)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if(owner==null || players.size()==0) return null;
        return new GlobalParty(players, owner);
    }

//    private void sendPartyRequest(Player p){
//        ByteArrayDataOutput o = ByteStreams.newDataOutput();
//        o.writeUTF("CPAF");
//        o.writeUTF("GetParty");
//        p.sendPluginMessage(Commons.getInstance(), "BungeeCord", o.toByteArray());
//    }

//    private class PartyWaiter {
//        private final UUID uuid;
//        private final Consumer<List<UUID>> whenReceived;
//
//        public PartyWaiter(UUID uuid, Consumer<List<UUID>> whenReceived) {
//            this.uuid = uuid;
//            this.whenReceived = whenReceived;
//        }
//    }
}
