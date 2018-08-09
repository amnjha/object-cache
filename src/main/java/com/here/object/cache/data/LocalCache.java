package com.here.object.cache.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.here.object.cache.config.local.LocalCacheConfig;
import com.here.object.cache.exceptions.NonUniqueKeyException;

/**
 * 
 * @author amajha
 *
 * @param <T> The datatype that can be held by this class
 */
public class LocalCache<T>  implements DataCache<T>{
	private LocalCacheConfig cacheConfig;
	private Cache<String, T> localCache;
	private Cache<String, Collection<T>> collectionLocalCache;
	
	
	/**
	 * @param cacheConfig
	 */
	public LocalCache(LocalCacheConfig cacheConfig) {
		super();
		this.cacheConfig = cacheConfig;
		localCache = configureLocalCache();
		collectionLocalCache=configureCollectionCache();
	}


	private Cache<String, T> configureLocalCache() {
		return CacheBuilder.newBuilder().expireAfterWrite(cacheConfig.getExpirationInMs(), TimeUnit.MILLISECONDS).build();
	}
	
	private Cache<String, Collection<T>> configureCollectionCache(){
		return CacheBuilder.newBuilder().expireAfterWrite(cacheConfig.getExpirationInMs(), TimeUnit.MILLISECONDS).build();
	}


	@Override
	public T store(String key, T t) {
		if(!Objects.isNull(localCache.getIfPresent(key)))
			throw new NonUniqueKeyException("Key : "+key +" already present in the cache, to replace the value, use DataCache::replace() instead.");
		
		localCache.put(key, t);
		return t;
	}


	@Override
	public T get(String key) {
		return localCache.getIfPresent(key);
	}


	@Override
	public T replace(String key, T t) {
		localCache.put(key, t);
		return t;
	}


	@Override
	public boolean deleteIfPresent(String key) {
		T t= localCache.getIfPresent(key);
		localCache.invalidate(key);
		return t!=null;
	}
	
	

	@Override
	public boolean addToList(String listName, T value) {
		Collection<T> coll = collectionLocalCache.getIfPresent(listName);
		if(coll==null) {
			coll = new ArrayList<>();
		}
		else if(!(coll instanceof List)) {
			throw new RuntimeException("given key is not a list");
		}
		
		return coll.add(value);
	}


	@Override
	public List<T> getList(String listName) {
		Collection<T> coll = collectionLocalCache.getIfPresent(listName);
		if(coll ==null)
			return null;
		
		if(!(coll instanceof List)) {
			throw new RuntimeException("given key is not a list");
		}
		
		return new ArrayList<>(coll);
	}


	@Override
	public boolean addAllToList(String listName, Collection<T> t) {
		Collection<T> coll = collectionLocalCache.getIfPresent(listName);
		
		if(coll==null) {
			coll = new ArrayList<>();
		}
		
		else if(!(coll instanceof List)) {
			throw new RuntimeException("given key is not a list");
		}
		
		return coll.addAll(t);
	}


	@Override
	public boolean addToSet(String setName, T value) {
		Collection<T> coll = collectionLocalCache.getIfPresent(setName);
		if(coll==null) {
			coll = new HashSet<>();
		}
		
		else if(!(coll instanceof Set)) {
			throw new RuntimeException("given key is not a set");
		}
		
		return coll.add(value);
	}


	@Override
	public Set<T> getSet(String setName) {
		Collection<T> coll = collectionLocalCache.getIfPresent(setName);
		if(coll==null) 
			return null;
		
		if(!(coll instanceof Set)) {
			throw new RuntimeException("given key is not a set");
		}
		
		return new HashSet<>(coll);
	}


	@Override
	public boolean addAllToSet(String setName, Collection<T> t) {
		Collection<T> coll = collectionLocalCache.getIfPresent(setName);
		if(coll==null) {
			coll = new HashSet<>();
		}
		
		else if(!(coll instanceof Set)) {
			throw new RuntimeException("given key is not a set");
		}
		
		return coll.addAll(t);
	}


	@Override
	public boolean removeFromSet(String setName, T value) {
		Collection<T> coll = collectionLocalCache.getIfPresent(setName);
		if(!(coll instanceof Set)) {
			throw new RuntimeException("given key is not a set");
		}
		
		return coll.remove(value);
	}


	@Override
	public boolean removeFromList(String listName, T value) {
		Collection<T> coll = collectionLocalCache.getIfPresent(value);
		if(!(coll instanceof List)) {
			throw new RuntimeException("given key is not a list");
		}
		
		return coll.remove(value);
	}


	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		localCache.invalidateAll();
		localCache.cleanUp();
	}
	
	
}
