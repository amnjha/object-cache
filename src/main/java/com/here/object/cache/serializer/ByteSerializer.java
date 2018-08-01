package com.here.object.cache.serializer;

import java.io.Serializable;

import org.apache.commons.lang3.SerializationUtils;

public class ByteSerializer {
	public static byte[] serialize(Serializable object) {
		byte[] serializedBytes= SerializationUtils.serialize(object);
		return serializedBytes;
	}
	
	public static <T> T deserizalize(byte[] objectData) {
		return SerializationUtils.deserialize(objectData);
	}
}
