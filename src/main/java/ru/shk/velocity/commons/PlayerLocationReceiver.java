package ru.shk.velocity.commons;

import com.google.gson.JsonObject;
import com.velocitypowered.api.proxy.Player;
import io.netty.util.concurrent.DefaultThreadFactory;
import ru.shk.commons.utils.Coordinates;
import ru.shk.commons.utils.Logger;
import ru.shk.commons.utils.redis.RedisCredentials;
import ru.shk.commons.utils.redis.channels.ChannelListener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

public class PlayerLocationReceiver {
    public Map<UUID, Coordinates> receivedCoordinates = new ConcurrentHashMap<>();
    public ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3, new DefaultThreadFactory("Commons Location Receiver Pool"));
    private final Commons pl;
    private int awaiting = 0;

    public PlayerLocationReceiver(Commons pl){
        this.pl = pl;
    }

    protected void receivedLocation(UUID uuid, String world, int x, int y, int z){
        receivedCoordinates.put(uuid, new Coordinates(world, x, y, z));
    }

    public Coordinates findPlayer(Player pp){
        awaiting++;
        sendFindPlayer(pp);
        int a = 0;
        while (a<5+Math.min(1+awaiting, 30)){
            a++;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(receivedCoordinates.containsKey(pp.getUniqueId())){
                Coordinates c = receivedCoordinates.get(pp.getUniqueId());
                if(c.getWorld().equals("player-not-found-error")){
                    sendFindPlayer(pp);
                    continue;
                }
                receivedCoordinates.remove(pp.getUniqueId());
                awaiting--;
                return c;
            }
        }
        awaiting--;
        return null;
    }

    private void sendFindPlayer(Player p){
        JsonObject o = new JsonObject();
        o.addProperty("type", "locationrequest");
        o.addProperty("playerUUID", p.getUniqueId().toString());
        pl.jedis().publish(Commons.REDIS_GENERAL_CHANNEL, o.toString());
    }

    public void findPlayer(Player pp, Consumer<Coordinates> whenDone){
        executor.submit(() -> {
            try {
                whenDone.accept(findPlayer(pp));
            } catch (Throwable t){
                t.printStackTrace();
            }
        });
    }

}
