package ru.shk.commons.utils.redis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Connection;
import redis.clients.jedis.JedisPooled;

public record RedisCredentials(String host, int port, String user, String password) {
    public static RedisCredentials DEFAULT = null;

    public RedisCredentials(String host, int port){
        this(host,port, null, null);
    }

    public JedisPooled newJedis(int minIdle, int maxIdle, int maxTotal){
        GenericObjectPoolConfig<Connection> conf = new GenericObjectPoolConfig<>();
        conf.setMinIdle(minIdle);
        conf.setMaxIdle(maxIdle);
        conf.setMaxTotal(maxTotal);
        if (user==null && password==null) return new JedisPooled(conf, host, port, 20000,false);
        return new JedisPooled(conf, host, port, 20000, user, password, false);
    }
}
