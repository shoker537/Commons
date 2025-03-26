package ru.shk.commons.utils.redis.channels;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.experimental.Accessors;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisPubSub;
import ru.shk.commons.utils.Logger;
import ru.shk.commons.utils.redis.RedisCredentials;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
@Accessors(fluent = true)
public class ChannelListener extends JedisPubSub {
    private static final Gson gson = new Gson();
    private final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
    @Getter private final String channel;
    private final Consumer<JsonObject> onMessage;
    private JedisPooled jedis;
    private boolean active = true;

    public ChannelListener(RedisCredentials redisCredentials, String channel, Consumer<JsonObject> onMessage) {
        this.channel = channel;
        this.onMessage = onMessage;
        new Thread(() -> {
            while (active) {
                jedis = redisCredentials.newJedis(1,1,1);
                jedis.subscribe(this, channel);
            }
        }).start();
    }

    public void shutdown(){
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        active = false;
        if (jedis==null) return;
        try {
            unsubscribe();
        } catch (Throwable t){

        }
        try {
            jedis.close();
        } catch (Throwable r){

        }
    }

    @Override
    public void onMessage(String channel, String message) {
        threadPool.submit(() -> {
            try {
                JsonObject o = gson.fromJson(message, JsonObject.class);
                onMessage.accept(o);
            } catch (Throwable t){
                Logger.warning("Error processing message from redis:");
                t.printStackTrace();
            }
        });
    }
}
