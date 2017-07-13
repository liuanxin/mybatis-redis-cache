package com.github.liuanxin.caches;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.cache.CacheException;

public final class SerializeUtil {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String jsonSerialize(Object obj) {
        if (obj == null) return null;

        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }
    public static Object unSerializeJson(String value) {
        if (value == null || value.length() == 0) return null;

        try {
            return OBJECT_MAPPER.readValue(value, Object.class);
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /*
    public static byte[] serialize(Object obj) {
		if (obj == null) return null;

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			new ObjectOutputStream(baos).writeObject(obj);
			return baos.toByteArray();
		} catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public static Object unSerialize(byte[] bytes) {
		if (bytes == null) return null;

		try {
			return new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
		} catch (Exception e) {
			throw new CacheException(e);
		}
	}
	*/
}
