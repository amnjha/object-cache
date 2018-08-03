package com.here.object.cache.data;

import com.here.object.cache.exceptions.NonUniqueKeyException;
import com.here.object.cache.exceptions.ObjectNotSerialzableException;

/**
 * 
 * @author amajha
 *
 * @param <T> The Data type to be stored inside the cache
 */
public interface DataCache<T> {
	
	/**
	 * Store Object in the Cache
	 * @param key {@link String} Key against which the given object will be stored
	 * @param t The value to be stored
	 * @return The stored Value
	 * @throws ObjectNotSerialzableException when the object that is being stored cannot be serialized
	 * @throws NonUniqueKeyException if the given key is already present in the cache
	 */
	public T store(String key, T t);
	
	/**
	 * Fetch the value from the cache for a given key
	 * @param key {@link String} The key for which the value is to be fetched
	 * @return The fetched object from the cache
	 */
	public T get(String key);
	
	/**
	 * Replace the value stored in the cache with the new Value. New Object is created in the cache if the key does not exist already
	 * @param key The key against which the value is to be replaced
	 * @param t the new value
	 * @return The stored value
	 * @throws ObjectNotSerialzableException when the object that is being stored cannot be serialized
	 */
	public default T replace(String key, T t) {
		throw new AbstractMethodError("Method Unimplemented by implementing class");
	}
	
	/**
	 * Delete the value associated with the given key in the cache. This does nothing if the value is not found.
	 * @param key The key against which the value is to be stored.
	 * @return {@link Boolean} <code>true</code> if object was deleted, <code>false</code> if the key is not present in the cache.
	 */
	public boolean deleteIfPresent(String key);

	/**
	 * Get Atomic Counter shared through the cache
	 * @return
	 */
	public default AtomicCounter getSharedAtomicCounter() {
		throw new AbstractMethodError("Method not implemented in the used cache. Method call unexpected");
	}
}
