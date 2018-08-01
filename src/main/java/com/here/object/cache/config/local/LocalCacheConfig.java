package com.here.object.cache.config.local;

import java.util.concurrent.TimeUnit;

import com.here.object.cache.config.CacheConfig;

/**
 * 
 * @author amajha
 *
 */
public class LocalCacheConfig implements CacheConfig {
	
	private long expirationInMs;

	public LocalCacheConfig(long cacheValidDurattion, TimeUnit timeUnit) {
		expirationInMs = TimeUnit.MILLISECONDS.convert(cacheValidDurattion, timeUnit);
	}

	/**
	 * @return the expirationInMs
	 */
	public long getExpirationInMs() {
		return expirationInMs;
	}
	
}
