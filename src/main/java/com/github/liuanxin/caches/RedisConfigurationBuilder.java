package com.github.liuanxin.caches;

import org.apache.ibatis.cache.CacheException;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

final class RedisConfigurationBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(RedisConfigurationBuilder.class);

    private static final String SPRING_FILE_NAME;
    static {
        String profile = System.getProperty("spring.profiles.active");
        SPRING_FILE_NAME = "application" + (isNotBlank(profile ) ? ("-" + profile) : "");
    }

	private static final String SPRING_CONFIG = "spring";
	private static final String SPRING_REDIS_CONFIG = "redis";
	private static final String SPRING_ADD_REDIS_CONFIG = SPRING_CONFIG + "." + SPRING_REDIS_CONFIG;

	private static RedisConfig DEFAULT_REDIS = null;
	public static RedisConfig config(Class clazz) {
		if (DEFAULT_REDIS != null) return DEFAULT_REDIS;

		// load application.yml
        String ymlFileName = SPRING_FILE_NAME + ".yml";
        Map<String, Object> configParam = loadYml(clazz, ymlFileName);
        DEFAULT_REDIS = setConfigProperties(configParam);
        if (isNotBlank(DEFAULT_REDIS)) {
            return DEFAULT_REDIS;
        }

        // load application.properties
        String propertyFileName = SPRING_FILE_NAME + ".properties";
        configParam = loadSpringBootProperties(clazz, propertyFileName);
        DEFAULT_REDIS = setConfigProperties(configParam);
        if (isNotBlank(DEFAULT_REDIS)) {
            return DEFAULT_REDIS;
        }

        // load redis.properties
        String redisFileName = "redis.properties";
        configParam = loadProperties(clazz, redisFileName);
        DEFAULT_REDIS = setConfigProperties(configParam);
        if (isNotBlank(DEFAULT_REDIS)) {
            return DEFAULT_REDIS;
        }

		// if no config, use default
        DEFAULT_REDIS = new RedisConfig();
		return DEFAULT_REDIS;
	}

    private static boolean isBlank(Object obj) {
        return obj == null || obj.toString().trim().length() == 0;
    }
    private static boolean isNotBlank(Object obj) {
        return !isBlank(obj);
    }
    private static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.size() == 0;
    }

	@SuppressWarnings("unchecked")
	private static Map<String, Object> loadYml(Class clazz, String ymlFileName) {
		try (InputStream input = clazz.getClassLoader().getResourceAsStream(ymlFileName)) {
			if (input != null) {
                Map<String, Object> configMap = (Map<String, Object>) new Yaml().load(input);
                if (isEmpty(configMap)) return null;

                Object config = configMap.get(SPRING_ADD_REDIS_CONFIG);
                if (isBlank(config)) {
                    config = configMap.get(SPRING_CONFIG);
                    if (config != null) {
                        config = ((Map<String, Object>) config).get(SPRING_REDIS_CONFIG);
                    }
                }
                if (isNotBlank(config)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("load yml file ({}) ({})", ymlFileName, config);
                    }
                    return (Map<String, Object>) config;
                } else {
                    Map<String, Object> configParam = configSpringBootParam(configMap);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("one line one config, load yml file ({}) return ({})", ymlFileName, configParam);
                    }
                    return configParam;
                }
			}
		} catch (Exception e) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("can not load yml file : %s", ymlFileName), e);
			}
		}
		return null;
	}
    private static Map<String, Object> loadSpringBootProperties(Class clazz, String propertyFileName) {
        try (InputStream input = clazz.getClassLoader().getResourceAsStream(propertyFileName)) {
            if (input != null) {
                Properties config = new Properties();
                config.load(input);
                Map<String, Object> configParam = configSpringBootParam(config);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("load properties file ({}) return ({})", propertyFileName, configParam);
                }
                return configParam;
            }
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("can not load property file : %s", propertyFileName), e);
            }
        }
        return null;
    }
	private static Map<String, Object> configSpringBootParam(Map<?, ?> configMap) {
        // if one line one config. like ==> spring.redis.host : 127.0.0.1
        Map<String, Object> paramMap = new HashMap<String, Object>();
        for (Map.Entry<?, ?> entry : configMap.entrySet()) {
            String key = entry.getKey().toString();
            if (key.startsWith(SPRING_ADD_REDIS_CONFIG + ".")) {
                paramMap.put(key.substring(key.lastIndexOf(".") + 1), entry.getValue());
            }
        }
        return paramMap;
    }

	private static RedisConfig setConfigProperties(Map<String, Object> configParam) {
	    if (isEmpty(configParam)) return null;

		RedisConfig redisConfig = new RedisConfig();
		MetaObject metaCache = SystemMetaObject.forObject(redisConfig);
		for (Map.Entry<String, Object> entry : configParam.entrySet()) {
			String name = entry.getKey();
			Object entryValue = entry.getValue();
			if (name != null && name.trim().length() > 0 && entryValue != null) {
				String value = entryValue.toString();
				if (metaCache.hasSetter(name)) {
					Class<?> type = metaCache.getSetterType(name);
					if (String.class == type) {
						metaCache.setValue(name, value);
					} else if (int.class == type || Integer.class == type) {
						metaCache.setValue(name, Integer.valueOf(value));
					} else if (long.class == type || Long.class == type) {
						metaCache.setValue(name, Long.valueOf(value));
					} else if (short.class == type || Short.class == type) {
						metaCache.setValue(name, Short.valueOf(value));
					} else if (byte.class == type || Byte.class == type) {
						metaCache.setValue(name, Byte.valueOf(value));
					} else if (float.class == type || Float.class == type) {
						metaCache.setValue(name, Float.valueOf(value));
					} else if (boolean.class == type || Boolean.class == type) {
						metaCache.setValue(name, Boolean.valueOf(value));
					} else if (double.class == type || Double.class == type) {
						metaCache.setValue(name, Double.valueOf(value));
					} else {
						throw new CacheException("Unsupported property type: '" + name + "' of type " + type);
					}
				}
			}
		}
		return redisConfig;
	}

    private static Map<String, Object> loadProperties(Class clazz, String propertyFileName) {
        try (InputStream input = clazz.getClassLoader().getResourceAsStream(propertyFileName)) {
            if (input != null) {
                Properties config = new Properties();
                config.load(input);

                Map<String, Object> map = new HashMap<String, Object>();
                for (Map.Entry<Object, Object> entry : config.entrySet()) {
                    map.put(entry.getKey().toString(), entry.getValue());
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("load redis.properties file ({}) return ({})", propertyFileName, map);
                }
                return map;
            }
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("can not load redis.property file : %s", propertyFileName), e);
            }
        }
        return null;
    }
}
