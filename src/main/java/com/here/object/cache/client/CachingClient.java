/**
 * 
 */
package com.here.object.cache.client;

import java.util.Objects;

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
	private ObjectCacheClientConfig clientConfig;

	/**
	 * 
	 */
	public CachingClient(ObjectCacheClientConfig cacheConfig) {
		super();
		this.clientConfig= Objects.requireNonNull(cacheConfig,"Cache Config cannot be null");
	}
	
	private DataCache<T> buildCache(){
		if(CachingMode.LOCAL_JVM_CACHE.equals(clientConfig.getCachingMode())) {
			LocalCacheConfig localCacheConfig = (LocalCacheConfig) clientConfig.getCacheConfig();
			cache= new LocalCache<>(localCacheConfig);
		}
		else if(CachingMode.STAND_ALONE_REDIS_CACHE.equals(clientConfig.getCachingMode())|| CachingMode.AWS_ELASTICACHE.equals(clientConfig.getCachingMode())) {
			RedisCacheConfig redisCacheConfig= (RedisCacheConfig) clientConfig.getCacheConfig();
			cache= new RedisCache<>(redisCacheConfig);
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
}
