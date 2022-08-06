package ru.shk.commonsbungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import ru.shk.commons.utils.Coordinates;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

public class PlayerLocationReceiver {
    public HashMap<UUID, Coordinates> receivedCoordinates = new HashMap<>();
    public ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
    private final Commons pl;
    private int awaiting = 0;

    public PlayerLocationReceiver(Commons pl){
        this.pl = pl;
    }

    protected void receivedLocation(UUID uuid, String world, int x, int y, int z){
        receivedCoordinates.put(uuid, new Coordinates(world, x, y, z));
    }

    public Coordinates findPlayer(ProxiedPlayer pp){
        awaiting++;
        pl.sendFindPlayer(pp);
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
                    pl.sendFindPlayer(pp);
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

    public void findPlayer(ProxiedPlayer pp, Consumer<Coordinates> whenDone){
        executor.submit(() -> whenDone.accept(findPlayer(pp)));
    }

}
