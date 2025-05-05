package com.example.chat.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtil {
    private static final Logger log = LoggerFactory.getLogger(RedisUtil.class);
    
    private static JedisPool jedisPool;
    
    /**
     * 初始化Redis连接池
     */
    public static void init(String host, int port, String password) {
        try {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(100);
            config.setMaxIdle(20);
            config.setMinIdle(5);
            // 使用新的方法替代已弃用的setMaxWaitMillis
            config.setMaxWait(java.time.Duration.ofMillis(10000));
            config.setTestOnBorrow(true);
            
            if (password != null && !password.isEmpty()) {
                jedisPool = new JedisPool(config, host, port, 2000, password);
            } else {
                jedisPool = new JedisPool(config, host, port, 2000);
            }
            
            log.info("Redis连接池初始化成功: {}:{}", host, port);
        } catch (Exception e) {
            log.error("Redis连接池初始化失败", e);
        }
    }
    
    /**
     * 获取Jedis实例
     */
    public static Jedis getJedis() {
        try {
            if (jedisPool != null) {
                return jedisPool.getResource();
            } else {
                log.error("Redis连接池未初始化");
                return null;
            }
        } catch (Exception e) {
            log.error("获取Jedis实例失败", e);
            return null;
        }
    }
    
    /**
     * 关闭Jedis连接
     */
    public static void close(Jedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }
    
    /**
     * 关闭连接池
     */
    public static void shutdown() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
}
