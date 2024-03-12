package com.here.object.cache.serializer;

import java.io.Serializable;

import org.apache.commons.lang3.SerializationUtils;

/**
 * The class used by this library to serialize and deserialize objects
 * 
 * @author amajha
 *
 */
public class ByteSerializer implements Serializer{
	public byte[] serialize(Serializable object) {
		byte[] serializedBytes= SerializationUtils.serialize(object);
		return serializedBytes;
	}
	
	public <T> T deserialize(byte[] objectData) {
		return SerializationUtils.deserialize(objectData);
	}
}
