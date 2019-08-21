package com.github.liuanxin.caches;

import org.apache.ibatis.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

/**
 * <pre>
 * usr redis cache in mybatis, use RedisTemplate in spring context
 *
 * 1. add com.github.liuanxin.caches.RedisContextUtils in spring context
 * &#064;Configuration
 * public class MybatisCacheConfig {
 *
 *   &#064;Bean
 *   public com.github.liuanxin.caches.RedisContextUtils setupCacheContext() {
 *     return new com.github.liuanxin.caches.RedisContextUtils();
 *   }
 * }
 *
 * 2. add &lt;cache type="com.github.liuanxin.caches.MybatisRedisCache" /&gt; in mapper.xml.
 * </pre>
 */
public class MybatisRedisCache implements Cache {

    private static final Logger LOGGER = LoggerFactory.getLogger(MybatisRedisCache.class);

    private static final Pattern COMMA_REGEX = Pattern.compile(", ");
    private static final String COMMA = ",";
    private static final Pattern BLANK_REGEX = Pattern.compile("\\s{2,}");
    private static final String BLANK = " ";

    private static final byte[] NIL_CACHE = "".getBytes();

    private static RedisTemplate<Object, Object> redisTemplate;


    private final String id;
    private final ReadWriteLock readWriteLock;

    public MybatisRedisCache(final String id) {
        if (id == null) {
            throw new IllegalArgumentException("Cache instances require an ID");
        }
        this.id = handleBlank(id);
        this.readWriteLock = new ReentrantReadWriteLock();
    }

    private String handleBlank(String str) {
        // <multi space> replace to <one space>
        // <, >          replace to <,>
        String s = str;
        s = BLANK_REGEX.matcher(s).replaceAll(BLANK);
        s = COMMA_REGEX.matcher(s).replaceAll(COMMA);
        return s;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public int getSize() {
        RedisTemplate<Object, Object> redisTemplate = getRedis();
        if (redisTemplate != null) {
            try {
                return redisTemplate.opsForHash().size(id.getBytes()).intValue();
            } catch (RedisConnectionFailureException e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(e.getMessage());
                }
            }
        }
        return 0;
    }

    @Override
    public void putObject(final Object key, final Object value) {
        if (key != null) {
            RedisTemplate<Object, Object> redisTemplate = getRedis();
            if (redisTemplate != null) {
                String k = handleBlank(key.toString());
                try {
                    // avoid cache penetration
                    byte[] v = (value == null) ? NIL_CACHE : JdkSerializer.serialize(value);
                    redisTemplate.opsForHash().put(id.getBytes(), k.getBytes(), v);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("put query result ({}) to cache", (id + "<>" + k));
                    }
                } catch (RedisConnectionFailureException e) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn(e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public Object getObject(final Object key) {
        if (key != null) {
            RedisTemplate<Object, Object> redisTemplate = getRedis();
            if (redisTemplate != null) {
                String k = handleBlank(key.toString());
                try {
                    Object value = redisTemplate.opsForHash().get(id.getBytes(), k.getBytes());
                    if (value instanceof byte[]) {
                        byte[] bytes = (byte[]) value;
                        // avoid cache penetration
                        if (Arrays.equals(NIL_CACHE , bytes)) {
                            return null;
                        }
                        Object result = JdkSerializer.unSerialize(bytes);
                        if (result != null) {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("get query result ({}) from cache", (id + "<>" + k));
                            }
                            return result;
                        }
                    }
                } catch (RedisConnectionFailureException e) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn(e.getMessage());
                    }
                }
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("RedundantCast")
    public Object removeObject(final Object key) {
        if (key != null) {
            RedisTemplate<Object, Object> redisTemplate = getRedis();
            if (redisTemplate != null) {
                String k = handleBlank(key.toString());
                try {
                    Object obj = redisTemplate.opsForHash().delete(id.getBytes(), (Object) k.getBytes());
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("remove query result ({}) from cache", (id + "<>" + k));
                    }
                    return obj;
                } catch (RedisConnectionFailureException e) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn(e.getMessage());
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void clear() {
        RedisTemplate<Object, Object> redisTemplate = getRedis();
        if (redisTemplate != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("clear query result ({}) from cache", id);
            }
            try {
                redisTemplate.delete(id.getBytes());
            } catch (RedisConnectionFailureException e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(e.getMessage());
                }
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

    private RedisTemplate<Object, Object> getRedis() {
        if (redisTemplate == null) {
            redisTemplate = RedisContextUtils.getRedisTemplate();
        }
        return redisTemplate;
    }
}
