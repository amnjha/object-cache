package com.here.object.cache.config.redis;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.here.object.cache.config.CacheConfig;
import com.here.object.cache.config.CachingMode;
import com.here.object.cache.exceptions.InvalidConfigException;
import com.here.object.cache.serializer.ByteSerializer;
import com.here.object.cache.serializer.Serializer;

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

	private String cacheId;
	private final List<String> redisServers;
	private final RedisConnectionType redisConnectionType;
	private final CachingMode cachingMode;
	private boolean enableLocalCaching;
	private int localCacheSize;
	private long expirationInMs;
	private Serializer serializer = new ByteSerializer();


	/**
	 * Creates a new config for the redis servers
	 * @param cachingMode {@link CachingMode}
	 * @param servers The server addresses that needs to be used for the cache
	 */
	public RedisCacheConfig(CachingMode cachingMode,ServerAddress...servers) {
		int serverCount= servers.length;
		if(serverCount<1)
			throw new InvalidConfigException("At-least one server is required while using the redis caching mode");

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
			throw new InvalidConfigException("At-least one server is required while using the redis caching mode");
		
		redisServers = Arrays.stream(servers).map(ServerAddress::getConnectionString).collect(toList());
		redisConnectionType= serverCount>1?RedisConnectionType.CLUSTER_CONNECTION:RedisConnectionType.SINGLE_SERVER;
		this.cachingMode= cachingMode;
		this.enableLocalCaching=enableLocalCaching;
	}

	/**
	 * Creates a new config for the redis servers
	 * @param cacheId The Cache Id for this cache, this is used for dividing the cache into namespaces
	 * @param cachingMode {@link CachingMode}
	 * @param enableLocalCaching enables local cache for faster retrieval, use this option only if your cache is not write intensive, back sync is not supported while using local cache
	 * @param servers The server addresses that needs to be used for the cache
	 */
	public RedisCacheConfig(String cacheId, CachingMode cachingMode,boolean enableLocalCaching, ServerAddress...servers) {
		int serverCount= servers.length;
		if(serverCount<1)
			throw new InvalidConfigException("At-least one server is required while using the redis caching mode");

		this.cacheId = cacheId;
		redisServers = Arrays.stream(servers).map(ServerAddress::getConnectionString).collect(toList());
		redisConnectionType= serverCount > 1 ? RedisConnectionType.CLUSTER_CONNECTION:RedisConnectionType.SINGLE_SERVER;
		this.cachingMode= cachingMode;
		this.enableLocalCaching=enableLocalCaching;
	}

	/**
	 * This Expires the element in the cache after the specified time from the time element is first inserted
	 * @param timeToLive
	 * @param timeUnit
	 */
	public void withTTL(long timeToLive, TimeUnit timeUnit){
			this.expirationInMs = TimeUnit.MILLISECONDS.convert(timeToLive, timeUnit);
	}

	/**
	 * Enables local cache for faster retrieval, use this option only if your cache is not write intensive, back sync is not supported while using local cache
	 * @param cacheSize the max number of elements to be stored on the cache
	 */
	public void withLocalCache(int cacheSize){
		this.enableLocalCaching=true;
		this.localCacheSize= cacheSize;
	}

	public void withCustomSerializer(Serializer serializer){
		this.serializer = serializer;
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

	public String getCacheId() {
		return cacheId;
	}

	public void setCacheId(String cacheId) {
		this.cacheId = cacheId;
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

	/**
	 * The cache valid duration
	 * @return the duration
	 */
	public long getExpirationInMs() {
		return expirationInMs;
	}

	public Serializer getSerializer() {
		return serializer;
	}
}
