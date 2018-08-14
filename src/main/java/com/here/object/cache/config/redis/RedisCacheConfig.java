package com.here.object.cache.config.redis;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;

import com.here.object.cache.config.CacheConfig;
import com.here.object.cache.config.CachingMode;
import com.here.object.cache.exceptions.InvalidConfigException;

/**
 * 
 * @author amajha
 *
 */
public class RedisCacheConfig implements CacheConfig{
	
	public enum RedisConnectionType{
		SINGLE_SERVER,
		CLUSTER_CONNECTION;
	}
	
	private final List<String> redisServers;
	private final RedisConnectionType redisConnectionType;
	private final CachingMode cachingMode;
	private boolean enableLocalCaching;
	private int localCacheSize;


	/**
	 * Creates a new config for the redis servers
	 * @param cachingMode {@link CachingMode}
	 * @param servers The server addresses that needs to be used for the cache
	 */
	public RedisCacheConfig(CachingMode cachingMode,ServerAddress...servers) {
		int serverCount= servers.length;
		if(serverCount<1)
			throw new InvalidConfigException("Atleast one server is required while using the redis caching mode");

		redisServers = Arrays.stream(servers).map(ServerAddress::getConnectionString).collect(toList());
		redisConnectionType= serverCount>1?RedisConnectionType.CLUSTER_CONNECTION:RedisConnectionType.SINGLE_SERVER;
		this.cachingMode= cachingMode;
		this.enableLocalCaching=false;
	}

	/**
	 * Creates a new config for the redis servers
	 * @param cachingMode {@link CachingMode}
	 * @param enableLocalCaching enables local cache for faster retrieval, use this option only if your cache is not write intensive, back sync is not supported while using local cache
	 * @param servers The server addresses that needs to be used for the cache
	 */
	public RedisCacheConfig(CachingMode cachingMode,boolean enableLocalCaching, ServerAddress...servers) {
		int serverCount= servers.length;
		if(serverCount<1) 
			throw new InvalidConfigException("Atleast one server is required while using the redis caching mode");
		
		redisServers = Arrays.stream(servers).map(ServerAddress::getConnectionString).collect(toList());
		redisConnectionType= serverCount>1?RedisConnectionType.CLUSTER_CONNECTION:RedisConnectionType.SINGLE_SERVER;
		this.cachingMode= cachingMode;
		this.enableLocalCaching=enableLocalCaching;
	}

	/**
	 * Enables local cache for faster retrieval, use this option only if your cache is not write intensive, back sync is not supported while using local cache
	 * @param cacheSize the max number of elements to be stored on the cache
	 */
	public void withLocalCache(int cacheSize){
		this.enableLocalCaching=true;
		this.localCacheSize= cacheSize;
	}

	/**
	 * Enables using the local-caching with local cache size as 500
	 */
	public void withLocalCache(){
		this.enableLocalCaching=true;
		this.localCacheSize=500;
	}

	/**
	 * @return the redisServers
	 */
	public List<String> getRedisServers() {
		return redisServers;
	}

	/**
	 * @return the redisConnectionType
	 */
	public RedisConnectionType getRedisConnectionType() {
		return redisConnectionType;
	}

	@Override
	public CachingMode getCachingMode() {
		return this.cachingMode;
	}

	/**
	 * Check if the local caching is enabled
	 * @return
	 */
	public boolean isEnableLocalCaching() {
		return enableLocalCaching;
	}

	/**
	 * Get the local cache size
	 * @return Integer
	 */
	public int getLocalCacheSize() {
		return localCacheSize;
	}
}
