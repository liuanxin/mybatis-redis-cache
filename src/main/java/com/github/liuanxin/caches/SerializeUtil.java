package com.github.liuanxin.caches;

import org.apache.ibatis.cache.CacheException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class SerializeUtil {

	// private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public static byte[] serialize(Object object) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			new ObjectOutputStream(baos).writeObject(object);
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

	/*
	public static byte[] jsonSerialize(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new CacheException(e);
        }
    }
    public static Object unserializeJson(byte[] value) {
	    if (value == null || value.length == 0) return null;

        try {
            return OBJECT_MAPPER.readValue(value, Object.class);
        } catch (IOException e) {
            throw new CacheException(e);
        }
    }
	*/
}
