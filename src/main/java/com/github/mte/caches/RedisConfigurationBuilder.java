package com.github.mte.caches;

import org.apache.ibatis.cache.CacheException;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

final class RedisConfigurationBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(RedisConfigurationBuilder.class);

	private static RedisConfig DEFAULT_REDIS = null;
	public static RedisConfig config(Class clazz) {
		if (DEFAULT_REDIS != null) return DEFAULT_REDIS;

		String fileName = "application";
		String profile = System.getProperty("spring.profiles.active");
		List<String> ymlList = new ArrayList<String>();
		// load yml first
		if (profile == null || profile.trim().length() == 0) {
			ymlList.add(fileName + ".yml");
		} else {
			ymlList.add(fileName + "-" + profile + ".yml");
			ymlList.add(fileName + "_" + profile + ".yml");
		}
		for (String ymlFileName : ymlList) {
			RedisConfig redisConfig = setConfigProperties(loadYml(clazz, ymlFileName));
			if (redisConfig != null) {
				DEFAULT_REDIS = redisConfig;
				return redisConfig;
			}
		}

		// then load properties profile file
		List<String> propertyList = new ArrayList<String>();
		if (profile == null || profile.trim().length() == 0) {
			propertyList.add(fileName + ".properties");
		} else {
			propertyList.add(fileName + "-" + profile + ".properties");
			propertyList.add(fileName + "_" + profile + ".properties");
		}
		for (String propertyFileName : propertyList) {
			RedisConfig redisConfig = setConfigProperties(loadProperties(clazz, propertyFileName));
			if (redisConfig != null) {
				DEFAULT_REDIS = redisConfig;
				return redisConfig;
			}
		}

		// if no config, use default
		RedisConfig redisConfig = new RedisConfig();
		DEFAULT_REDIS = redisConfig;
		return redisConfig;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> loadYml(Class clazz, String ymlFileName) {
		Yaml yaml = new Yaml();
		try (InputStream input = clazz.getClassLoader().getResourceAsStream(ymlFileName)) {
			if (input != null) {
				return (Map<String, Object>) yaml.load(input);
			}
		} catch (IOException e) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("can not load yml file : %s", ymlFileName), e);
			}
		}
		return null;
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
				return map;
			}
		} catch (IOException e) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("can not load property file : %s", propertyFileName), e);
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static RedisConfig setConfigProperties(Map<String, Object> properties) {
		if (properties == null || properties.size() == 0) return null;

		Object redis = properties.get("spring.redis");
		if (redis == null) {
			redis = properties.get("spring");
			if (redis != null) {
				redis = ((Map<String, Object>) redis).get("redis");
			}
		}
		if (redis == null) return null;

		RedisConfig redisConfig = new RedisConfig();
		MetaObject metaCache = SystemMetaObject.forObject(redisConfig);
		for (Map.Entry<String, Object> entry : ((Map<String, Object>) redis).entrySet()) {
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
}
