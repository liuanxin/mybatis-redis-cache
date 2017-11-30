package com.github.liuanxin.caches;

import org.apache.ibatis.cache.CacheException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

final class SerializeUtil {

    static byte[] serialize(Object obj) {
        if (obj == null) return null;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(obj);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    static Object unSerialize(byte[] bytes) {
        if (bytes == null) return null;

        try {
            return new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }
}
