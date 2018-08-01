package com.here.object.cache.data;

public interface DataCache<T> {
	
	/**
	 * 
	 * @param key
	 * @param t
	 * @return
	 */
	public T store(String key, T t);
	
	/**
	 * 
	 * @param key
	 * @return
	 */
	public T get(String key);
	
	/**
	 * 
	 * @param key
	 * @param t
	 * @return
	 */
	public default T replace(String key, T t) {
		throw new AbstractMethodError("Method Unimplemented by implementing class");
	}
	
	/**
	 * 
	 * @param key
	 * @return
	 */
	public boolean deleteIfPresent(String key);
}
