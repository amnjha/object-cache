package com.here.object.cache.serializer;

import java.io.Serializable;

public interface Serializer {
	byte[] serialize(Serializable object);

	<T> T deserialize(byte[] objectData);
}
