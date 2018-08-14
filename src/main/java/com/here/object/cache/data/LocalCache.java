package com.here.object.cache.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
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
		return CacheBuilder.
				newBuilder().
				expireAfterWrite(cacheConfig.getExpirationInMs(), TimeUnit.MILLISECONDS).
				maximumSize(cacheConfig.getCacheSize()).
				build();
	}
	
	private Cache<String, Collection<T>> configureCollectionCache(){
		return CacheBuilder.
				newBuilder().
				expireAfterWrite(cacheConfig.getExpirationInMs(), TimeUnit.MILLISECONDS).
				maximumSize(cacheConfig.getCacheSize()).
				build();
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
	public List<T> getList(String listName) {
		Collection<T> coll = collectionLocalCache.getIfPresent(listName);
		if(coll ==null) {
			List<T> t= new CopyOnWriteArrayList<>();
			collectionLocalCache.put(listName, t);
			return t;
		}

		if(!(coll instanceof List)) {
			throw new RuntimeException("given key is not a list");
		}else{
			return (List<T>)coll;
		}
	}


	@Override
	public Set<T> getSet(String setName) {
		Collection<T> coll = collectionLocalCache.getIfPresent(setName);
		if(coll==null) {
			Set<T> t= new CopyOnWriteArraySet<>();
			collectionLocalCache.put(setName, t);
			return t;
		}
		
		if(!(coll instanceof Set)) {
			throw new RuntimeException("given key is not a set");
		}else{
			return (Set<T>) coll;
		}
	}


	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		localCache.invalidateAll();
		localCache.cleanUp();
	}
	
	
}
