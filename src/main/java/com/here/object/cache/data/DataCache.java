package com.here.object.cache.data;

import java.util.Collection;
import java.util.List;
import java.util.Set;

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
	
	/**
	 * Creates a new list if not already present, using the given name and adds the element to the queue
	 * @param listName {@link String}
	 * @param value The value to be stored
	 */
	public default boolean addToList(String listName, T value) {
		throw new AbstractMethodError("Method not implemented in the used cache. Method call unexpected");
	}

	/**
	 * Remove the given element from the list
	 * @param setName {@link String} The Name of the list from which the element would be removed
	 * @param value The value to be removed
	 * @return
	 */
	public default boolean removeFromList(String listName, T value) {
		throw new AbstractMethodError("Method not implemented in the used cache. Method call unexpected");
	}
	
	/**
	 * Get all elements of the list
	 * <b>Note: </b> changes made to the returned list does not impact the original list
	 * @param listName the list name to get
	 * @return
	 */
	public default List<T> getList(String listName){
		throw new AbstractMethodError("Method not implemented in the used cache. Method call unexpected");
	}

	/**
	 * Add all element of the collection to a list in the cache
	 * @param listName {@link String} The name of the list to which the value must be added
	 * @param t the Value to be added
	 * @return whether the operation was successful or not
	 */
	public default boolean addAllToList(String listName, Collection<T> t){
		throw new AbstractMethodError("Method not implemented in the used cache. Method call unexpected");
	}

	/**
	 * Creates a new set if not already present, using the given name and adds the element to the queue
	 * @param listName {@link String}
	 * @param value The value to be stored
	 */
	public default boolean addToSet(String setName, T value){
		throw new AbstractMethodError("Method not implemented in the used cache. Method call unexpected");
	}

	/** 
	 * Get all elements of the set
	 * <b>Note: </b> changes made to the returned list does not impact the original list
	 * @param listName the list name to get
	 * @return
	 */
	public default Set<T> getSet(String setName){
		throw new AbstractMethodError("Method not implemented in the used cache. Method call unexpected");
	}

	/**
	 * Add all element of the collection to a set in the cache
	 * @param listName {@link String} The name of the set to which the value must be added
	 * @param t the Value to be added
	 * @return whether the operation was successful or not
	 */
	public default boolean addAllToSet(String setName, Collection<T> t){
		throw new AbstractMethodError("Method not implemented in the used cache. Method call unexpected");
	}

	/**
	 * Remove the given element from the set
	 * @param setName {@link String} The Name of the set from which the element would be removed
	 * @param value The value to be removed
	 * @return
	 */
	public default boolean removeFromSet(String setName, T value) {
		throw new AbstractMethodError("Method not implemented in the used cache. Method call unexpected");
	}

}
