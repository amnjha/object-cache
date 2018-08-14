package com.here.object.cache.config.local;

import java.util.concurrent.TimeUnit;

import com.here.object.cache.config.CacheConfig;
import com.here.object.cache.config.CachingMode;

/**
 * 
 * @author amajha
 *
 */
public class LocalCacheConfig implements CacheConfig {
	
	private long expirationInMs;
	private int cacheSize;

	/**
	 * Configuration for local cache
	 * @param cacheSize
	 * @param cacheValidDuration
	 * @param timeUnit
	 */
	public LocalCacheConfig(int cacheSize, long cacheValidDuration, TimeUnit timeUnit) {
		this.cacheSize=cacheSize;
		this.expirationInMs = TimeUnit.MILLISECONDS.convert(cacheValidDuration, timeUnit);
	}

	/**
	 * Configure the local cache, this cache can store at max {@link Integer}#MAX_VALUE elements.
	 * @param cacheValidDuration
	 * @param timeUnit
	 */
	public LocalCacheConfig(long cacheValidDuration, TimeUnit timeUnit){
		this.cacheSize=Integer.MAX_VALUE;
		this.expirationInMs = TimeUnit.MILLISECONDS.convert(cacheValidDuration, timeUnit);
	}

	/**
	 * The Max elements to be saved on the cache;
	 * @return
	 */
	public int getCacheSize() {
		return cacheSize;
	}

	/**
	 * @return the expirationInMs
	 */
	public long getExpirationInMs() {
		return expirationInMs;
	}

	@Override
	public CachingMode getCachingMode() {
		return CachingMode.LOCAL_JVM_CACHE;
	}
	
}
