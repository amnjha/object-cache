package com.here.object.cache.serializer;

import java.io.Serializable;

public interface Serializer {
	<T extends Serializable> byte[] serialize(T object);

	<T> T deserialize(byte[] objectData);
}
