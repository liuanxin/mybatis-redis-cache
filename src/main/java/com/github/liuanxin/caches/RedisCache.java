package com.github.liuanxin.caches;

import org.apache.ibatis.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

/**
 * <pre>
 * 基于 redis 的 mybatis 缓存. 自动从 spring 上下文中获取 RedisTemplate
 *
 * 只需要在 mapper.xml 中添加 &lt;cache type="com.github.liuanxin.caches.MybatisRedisCache" /&gt; 即可.
 * 前提是要将 com.github.liuanxin.caches.RedisContextUtils 放入 spring 的上下文
 *
 * &#064;Configuration
 * public class MybatisCacheConfig {
 *
 *   &#064;Bean
 *   public com.github.liuanxin.caches.RedisContextUtils setupApplicationContext() {
 *     return new com.github.liuanxin.caches.RedisContextUtils();
 *   }
 * }
 * </pre>
 */
public class RedisCache implements Cache {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisCache.class);

    private static final Pattern BLANK_REGEX = Pattern.compile("\\s{2,}");
    private static final String SPACE = " ";

    private static RedisTemplate<Object, Object> redisTemplate;

    private final String id;
    private final ReadWriteLock readWriteLock;

    public RedisCache(final String id) {
        if (id == null) {
            throw new IllegalArgumentException("Cache instances require an ID");
        }
        this.id = BLANK_REGEX.matcher(id).replaceAll(SPACE);
        this.readWriteLock = new ReentrantReadWriteLock();
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public int getSize() {
        RedisTemplate<Object, Object> redisTemplate = getRedis();
        return (redisTemplate == null) ? 0 : redisTemplate.opsForHash().size(id.getBytes()).intValue();
    }

    @Override
    public void putObject(final Object key, final Object value) {
        RedisTemplate<Object, Object> redisTemplate = getRedis();
        if (redisTemplate != null) {
            String keyHash = BLANK_REGEX.matcher(key.toString()).replaceAll(SPACE);
            redisTemplate.opsForHash().put(id.getBytes(), keyHash.getBytes(), SerializeUtil.serialize(value));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("put query result ({}) to cache", (id + "<>" + keyHash));
            }
        }
    }

    @Override
    public Object getObject(final Object key) {
        RedisTemplate<Object, Object> redisTemplate = getRedis();
        if (redisTemplate != null) {
            String keyHash = BLANK_REGEX.matcher(key.toString()).replaceAll(SPACE);
            Object value = redisTemplate.opsForHash().get(id.getBytes(), keyHash.getBytes());
            if (value != null && value instanceof byte[]) {
                Object result = SerializeUtil.unSerialize((byte[]) value);
                if (result != null) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("get query result ({}) from cache", (id + "<>" + keyHash));
                    }
                    return result;
                }
            }
        }
        return null;
    }

    @Override
    public Object removeObject(final Object key) {
        RedisTemplate<Object, Object> redisTemplate = getRedis();
        if (redisTemplate == null) {
            return null;
        }
        String keyHash = BLANK_REGEX.matcher(key.toString()).replaceAll(SPACE);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("remove query result ({}) from cache", (id + "<>" + keyHash));
        }
        return redisTemplate.opsForHash().delete(id.getBytes(), (Object) keyHash.getBytes());
    }

    @Override
    public void clear() {
        RedisTemplate<Object, Object> redisTemplate = getRedis();
        if (redisTemplate != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("remove query result ({}) from cache", id);
            }
            redisTemplate.opsForHash().delete(id.getBytes());
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

    private RedisTemplate<Object, Object> getRedis() {
        if (redisTemplate == null) {
            redisTemplate = RedisContextUtils.getRedisTemplate();
        }
        return redisTemplate;
    }
}
