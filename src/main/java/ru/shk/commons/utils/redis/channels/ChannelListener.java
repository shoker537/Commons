package ru.shk.commons.utils.redis.channels;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import ru.shk.commons.utils.redis.RedisCredentials;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
@Accessors(fluent = true)
public class ChannelListener {
    private static final Gson gson = new Gson();
    private final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
    @Getter private final String channel;
    private final Consumer<JsonObject> onMessage;
    private final RedisCredentials redisCredentials;
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final Thread thread;

    public ChannelListener(RedisCredentials redisCredentials, String channel, Consumer<JsonObject> onMessage) {
        this.channel = channel;
        this.onMessage = onMessage;
        this.redisCredentials = redisCredentials;
        thread = new Thread(this::startListening);
        thread.start();
    }

    public void startListening() {
        while (active.get()) {
            try (Jedis jedis = redisCredentials.newJedis()) {
                System.out.println("Подключение к Redis...");
                JedisPubSub jedisPubSub = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        if (!channel.equalsIgnoreCase(ChannelListener.this.channel)) return;
                        JsonObject o;
                        try {
                            o = gson.fromJson(message, JsonObject.class);
                        } catch (JsonSyntaxException e) {
                            return;
                        }
                        if (onMessage != null) {
                            if(!Bukkit.isStopping()) threadPool.submit(() -> onMessage.accept(o));
                        }
                    }
                };

                jedis.subscribe(jedisPubSub, channel);
            } catch (Exception e) {
                System.err.println("Ошибка соединения: " + e.getMessage());
                System.err.println("Повторная попытка подключения через 5 секунд...");
                try {
                    Thread.sleep(5000); // ждем перед повторной попыткой
                } catch (InterruptedException ie) {
                    return;
                }
            }
        }
    }

    public void shutdown(){
        active.set(false);
        if (thread!=null) thread.interrupt();
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
