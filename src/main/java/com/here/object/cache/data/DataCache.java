package com.here.object.cache.data;

import com.here.object.cache.builder.CacheBuilder;
import com.here.object.cache.exceptions.NonUniqueKeyException;
import com.here.object.cache.exceptions.ObjectNotSerialzableException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @param <T> The Data type to be stored inside the cache
 * @author amajha
 */
public interface DataCache<T> {

	static CacheBuilder builder() {
		return CacheBuilder.newBuilder();
	}

	/**
	 * Store Object in the Cache
	 *
	 * @param key {@link String} Key against which the given object will be stored
	 * @param t   The value to be stored
	 * @return The stored Value
	 * @throws ObjectNotSerialzableException when the object that is being stored cannot be serialized
	 * @throws NonUniqueKeyException         if the given key is already present in the cache
	 */
	public T store(String key, T t);

	/**
	 * Store Object in the Cache
	 *
	 * @param key        {@link String} Key against which the given object will be stored
	 * @param t          The value to be stored
	 * @param timeToLive The Time for which this object will live in cache, this will override all global TTL settings
	 * @param timeUnit   The time unit for timeToLive param
	 * @return The stored Value
	 * @throws ObjectNotSerialzableException when the object that is being stored cannot be serialized
	 * @throws NonUniqueKeyException         if the given key is already present in the cache
	 */
	public default T store(String key, T t, long timeToLive, TimeUnit timeUnit) {
		throw new AbstractMethodError("Method not implemented in the used cache");
	}

	/**
	 * Batch Replace the value stored in the cache with the new Value. New Objects are created in the cache if the key does not exist already
	 *  @param dataToInsert the map of key value pairs
	 * @param timeout      the timeout for this batch operation
	 * @param timeUnit     unit in which the batch operation timeout is specified
	 * @return
	 */
	default boolean storeBatch(Map<String, T> dataToInsert, long timeout, TimeUnit timeUnit){
		throw new AbstractMethodError("Method Unimplemented by used class");
	}

	/**
	 * Fetch the value from the cache for a given key
	 *
	 * @param key {@link String} The key for which the value is to be fetched
	 * @return The fetched object from the cache
	 */
	public T get(String key);

	/**
	 * Batch Replace the value stored in the cache with the new Value. New Objects are created in the cache if the key does not exist already
	 *  @param dataToInsert the map of key value pairs
	 * @param timeout      the timeout for this batch operation
	 * @param timeUnit     unit in which the batch operation timeout is specified
	 * @return
	 */
	default boolean replaceBatch(Map<String, T> dataToInsert, long timeout, TimeUnit timeUnit){
		throw new AbstractMethodError("Method Unimplemented by used class");
	}

	/**
	 * Replace the value stored in the cache with the new Value. New Object is created in the cache if the key does not exist already
	 *
	 * @param key The key against which the value is to be replaced
	 * @param t   the new value
	 * @return The stored value
	 * @throws ObjectNotSerialzableException when the object that is being stored cannot be serialized
	 */
	public default T replace(String key, T t) {
		throw new AbstractMethodError("Method Unimplemented by used class");
	}

	/**
	 * Replace the value stored in the cache with the new Value. New Object is created in the cache if the key does not exist already
	 *
	 * @param key        The key against which the value is to be replaced
	 * @param t          the new value
	 * @param timeToLive The Time for which this object will live in cache, this will override all global TTL settings
	 * @param timeUnit   The time unit for timeToLive param
	 * @return The stored value
	 * @throws ObjectNotSerialzableException when the object that is being stored cannot be serialized
	 */
	public default T replace(String key, T t, long timeToLive, TimeUnit timeUnit) {
		throw new AbstractMethodError("Method not implemented in the used cache");
	}

	/**
	 * Delete the value associated with the given key in the cache. This does nothing if the value is not found.
	 *
	 * @param key The key against which the value is to be stored.
	 * @return {@link Boolean} <code>true</code> if object was deleted, <code>false</code> if the key is not present in the cache.
	 */
	public boolean deleteIfPresent(String key);

	/**
	 * Get Atomic Counter shared through the cache
	 *
	 * @return
	 */
	public default AtomicLong getSharedAtomicCounter(String counterName) {
		throw new AbstractMethodError("Method not implemented in the used cache. Method call unexpected");
	}

	/**
	 * Creates a new set if not already present on the cache and returns the new/existing set
	 *
	 * @param setName the name against which the set is stored in the cache
	 * @return List of T
	 */
	public default Set<T> getSet(String setName) {
		throw new AbstractMethodError("Method not implemented in the used cache. Method call unexpected");
	}

	/**
	 * Creates a new list if not already present on the cache and returns the new/existing list
	 *
	 * @param listName the name against which the list is stored in the cache
	 * @return List of T
	 */
	public default List<T> getList(String listName) {
		throw new AbstractMethodError("Method not implemented in the used cache. Method call unexpected");
	}

	/**
	 * Get All Keys Stored in the Redis
	 *
	 * @return
	 */
	Set<String> getAllKeys();

	/**
	 * Get Keys as a scan iterator
	 * @param limit the number of keys to get
	 * @return ScanResult : the scan result pointing to the head of the result,
	 * 						call scanResult.getNextResult() to get the next results.
	 */
	default RedisCache.ScanResult scanAllKeys(int limit){
		throw new AbstractMethodError("Method not implemented in the used cache. Method call unexpected");
	}

	/**
	 * Get Keys stored in the redis by a given pattern
	 *
	 * @param keyPattern
	 * @return
	 */
	Set<String> getKeyListByPattern(String keyPattern);

	default RedisCache.ScanResult scanKeysByPattern(String keyPattern, int limit){
		throw new AbstractMethodError("Method not implemented in the used cache. Method call unexpected");
	}


	/**
	 * Delete all the values stored in redis with matching key pattern
	 *
	 * @param keyPattern the key pattern to match for
	 * @return the number of keys deleted
	 */
	long deleteByKeyPattern(String keyPattern);

	/**
	 * Delete the values store for the given keys
	 *
	 * @param keys the keys for which the values needs to be removed from the cache
	 * @return the number of values deleted
	 */
	long deleteByKeys(String... keys);


	/**
	 * Delete the Keys by key pattern in an async fashion
	 * @param keys the keys for which the values needs to be removed from the cache
	 * @return the mono for the number of keys which are deleted
	 */
	Mono<Long> deleteByKeysAsync(String... keys);

	/**
	 * Clean-up The Cache
	 */
	public default void purgeCache() {
		throw new AbstractMethodError("Method not implemented in the used cache. Method call unexpected");
	}


	public default Mono<String> purgeCacheAsync() {
		throw new AbstractMethodError("Method not implemented in the used cache. Method call unexpected");
	}
}
