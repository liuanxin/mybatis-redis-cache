package com.github.liuanxin.caches;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.RedisTemplate;

public class RedisContextUtils implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    @SuppressWarnings("unchecked")
    static RedisTemplate<Object, Object> getRedisTemplate() {
        return (RedisTemplate<Object, Object>) context.getBean("redisTemplate");
    }
}
