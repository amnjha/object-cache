package com.here.object.cache.data;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	public List<String> getAllKeys() {
		List<String> keys = new ArrayList<>();
		keys.addAll(localCache.asMap().keySet());
		keys.addAll(collectionLocalCache.asMap().keySet());
		return keys;
	}

	@Override
	public List<String> getKeyListByPattern(String keyPattern) {
		throw new RuntimeException("Method not supported on local cache");
	}

	@Override
	public long deleteByKeyPattern(String keyPattern) {
		return 0;
	}


	@Override
	public long deleteByKeys(String... keys) {
		long deleteCount=0;
		for(String key: keys){
			T t= localCache.getIfPresent(key);
			if(t!=null){
				localCache.invalidate(key);
				deleteCount++;
			}else if(collectionLocalCache.getIfPresent(key)!=null){
				collectionLocalCache.invalidate(key);
				deleteCount++;
			}
		}
		return deleteCount;
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
