package com.here.object.cache.data;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.here.object.cache.config.local.LocalCacheConfig;
import com.here.object.cache.exceptions.NonUniqueKeyException;

public class LocalCache<T>  implements DataCache<T>{
	private LocalCacheConfig cacheConfig;
	private Cache<String, T> localCache;
	
	
	/**
	 * @param cacheConfig
	 */
	public LocalCache(LocalCacheConfig cacheConfig) {
		super();
		this.cacheConfig = cacheConfig;
		localCache = configureLocalCache();
	}


	private Cache<String, T> configureLocalCache() {
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
	protected void finalize() throws Throwable {
		super.finalize();
		localCache.invalidateAll();
		localCache.cleanUp();
	}
	
	
}
