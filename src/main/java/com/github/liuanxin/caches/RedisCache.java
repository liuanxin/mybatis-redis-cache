package com.github.liuanxin.caches;


import org.apache.ibatis.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.locks.ReadWriteLock;

public final class RedisCache implements Cache {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConfigurationBuilder.class);

    private final ReadWriteLock readWriteLock = new DummyReadWriteLock();

    private String id;

    private static JedisPool pool;

    public RedisCache(final String id) {
        if (id == null) {
            throw new IllegalArgumentException("Cache instances require an ID");
        }
        this.id = id;

        RedisConfig redisConfig = RedisConfigurationBuilder.CONFIG;
        pool = new JedisPool(redisConfig, redisConfig.getHost(), redisConfig.getPort(),
                redisConfig.getConnectionTimeout(), redisConfig.getPassword());
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public int getSize() {
        try (Jedis jedis = pool.getResource()) {
            return jedis.hgetAll(id.getBytes()).size();
        } catch (Exception e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("getSize exception", e);
            }
            return 0;
        }
    }

    @Override
    public void putObject(final Object key, final Object value) {
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(id.getBytes(), key.toString().getBytes(), SerializeUtil.serialize(value));
        } catch (Exception e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("putObject exception", e);
            }
        }
    }

    @Override
    public Object getObject(final Object key) {
        try (Jedis jedis = pool.getResource()) {
            return SerializeUtil.unSerialize(jedis.hget(id.getBytes(), key.toString().getBytes()));
        } catch (Exception e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("getObject exception", e);
            }
            return null;
        }
    }

    @Override
    public Object removeObject(final Object key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.hdel(id.getBytes(), key.toString().getBytes());
        } catch (Exception e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("removeObject exception", e);
            }
            return null;
        }
    }

    @Override
    public void clear() {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(id.getBytes());
        } catch (Exception e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("clear exception", e);
            }
        }
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return readWriteLock;
    }

    @Override
    public String toString() {
        return "Redis {" + id + "}";
    }
}
