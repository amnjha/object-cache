package com.here.object.cache.client;

import java.util.Objects;
import java.util.function.Function;

import com.here.object.cache.config.CachingMode;
import com.here.object.cache.config.ObjectCacheClientConfig;
import com.here.object.cache.config.local.LocalCacheConfig;
import com.here.object.cache.config.redis.RedisCacheConfig;
import com.here.object.cache.data.DataCache;
import com.here.object.cache.data.LocalCache;
import com.here.object.cache.data.RedisCache;

/**
 * Client class for managing the cache
 * @author amajha
 *
 */
public class CachingClient<T> {
	private DataCache<T> cache;
	private final ObjectCacheClientConfig clientConfig;

	/**
	 * 
	 */
	public CachingClient(ObjectCacheClientConfig cacheConfig) {
		super();
		this.clientConfig= Objects.requireNonNull(cacheConfig,"Cache Config cannot be null");
	}
	
	private DataCache<T> buildCache(String... cacheId){
		String cache_id = null;
		if(cacheId.length!=0)
			cache_id = cacheId[0];

		if(CachingMode.LOCAL_JVM_CACHE.equals(clientConfig.getCachingMode())) {
			LocalCacheConfig localCacheConfig = (LocalCacheConfig) clientConfig.getCacheConfig();
			if(cache_id!=null) {
				cache = LocalCache.getCacheById(cache_id);
				if (cache == null)
					cache = new LocalCache<>(localCacheConfig, cache_id);
			}
			else
				cache = new LocalCache<>(localCacheConfig);
		}

		// Add support for cache ID in the Redis---- Required?
		else if(CachingMode.STAND_ALONE_REDIS_CACHE.equals(clientConfig.getCachingMode())|| CachingMode.AWS_ELASTICACHE.equals(clientConfig.getCachingMode())) {
			RedisCacheConfig redisCacheConfig= (RedisCacheConfig) clientConfig.getCacheConfig();
			cache= new RedisCache<>(redisCacheConfig);
		}
		return this.cache;
	}
	
	private DataCache<T> buildCache(Function<String, T> valueLoader, String... cacheId){
		String cache_id = null;
		if(cacheId.length!=0)
			cache_id = cacheId[0];

		if(CachingMode.LOCAL_JVM_CACHE.equals(clientConfig.getCachingMode())) {
			LocalCacheConfig localCacheConfig = (LocalCacheConfig) clientConfig.getCacheConfig();
			if(cache_id!=null) {
				cache = LocalCache.getCacheById(cache_id);
				if (cache == null)
					cache = new LocalCache<>(localCacheConfig, valueLoader, cache_id);
			}
			else
				cache = new LocalCache<>(localCacheConfig, valueLoader);
		}

		else if(CachingMode.STAND_ALONE_REDIS_CACHE.equals(clientConfig.getCachingMode())|| CachingMode.AWS_ELASTICACHE.equals(clientConfig.getCachingMode())) {
			RedisCacheConfig redisCacheConfig= (RedisCacheConfig) clientConfig.getCacheConfig();
			redisCacheConfig.setCacheId(cache_id);
			cache= new RedisCache<>(redisCacheConfig, valueLoader);
		}
		return this.cache;
	}
	
	/**
	 * This method builds the cache, if not already present and returns it
	 * @return {@link DataCache} The cache held by this client
	 */
	public DataCache<T> getCache(){
		if(cache==null) {
			synchronized (clientConfig) {
				if(cache==null) {
					buildCache();
				}
			}
		}
		return this.cache;
	}

	/**
	 * This method builds the cache, if not already present and returns it
	 * @return {@link DataCache} The cache held by this client
	 */
	public DataCache<T> getCache(String cacheId){
		if(cache==null) {
			synchronized (clientConfig) {
				if(cache==null) {
					buildCache(cacheId);
				}
			}
		}
		return this.cache;
	}
	
	/**
	 * This method builds the cache, if not already present and returns it
	 * @param valueLoader The loader that populates the cache if key is not present
	 * @return {@link DataCache} The cache held by this client
	 */
	public DataCache<T> getCache(Function<String, T> valueLoader){
		if(cache==null) {
			synchronized (clientConfig) {
				if(cache==null) {
					buildCache(valueLoader);
				}
			}
		}
		return this.cache;
	}

	/**
	 * This method builds the cache, if not already present and returns it
	 * @param valueLoader The loader that populates the cache if key is not present
	 * @param cacheId the id with which this cache needs to be created
	 * @return {@link DataCache} The cache held by this client
	 */
	public DataCache<T> getCache(Function<String, T> valueLoader, String cacheId){
		if(cache==null) {
			synchronized (clientConfig) {
				if(cache==null) {
					buildCache(valueLoader, cacheId);
				}
			}
		}
		return this.cache;
	}
}
