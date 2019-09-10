package com.here.object.cache.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import com.here.object.cache.client.CachingClient;
import com.here.object.cache.config.*;
import com.here.object.cache.config.redis.RedisCacheConfig;
import com.here.object.cache.data.DataCache;
import com.here.object.cache.exceptions.InvalidConfigException;
import com.here.object.cache.config.redis.ServerAddress;
import com.here.object.cache.serializer.Serializer;

/**
 * 
 * @author amajha
 *
 */
public class CacheBuilder {

	private CachingMode mode;
	private List<ServerAddress> serverAdresses;
	private long ttl;
	private TimeUnit ttlUnit;
	private boolean useLocalCache;
	private AmazonElastiCacheClient awsClient;
	private String cacheClusterId;
	private boolean useSSL;
	private String cacheId;
	private Serializer serializer;

	private CacheBuilder() {
	}

	public static CacheBuilder newBuilder() {
		return new CacheBuilder();
	}

	@Deprecated
	public static CacheBuilder builder(){
		return newBuilder();
	}

	public CacheBuilder withCachingMode(CachingMode cachingMode) {
		Objects.nonNull(cachingMode);
		this.mode = cachingMode;
		if (CachingMode.STAND_ALONE_REDIS_CACHE.equals(cachingMode) || CachingMode.AWS_ELASTICACHE.equals(cachingMode))
			this.serverAdresses = new ArrayList<>();
		return this;
	}

	public CacheBuilder withCacheId(String cacheId){
		this.cacheId = cacheId;
		return this;
	}

	public CacheBuilder withAwsClient(AmazonElastiCacheClient client, String clusterId) {
		this.awsClient = client;
		this.cacheClusterId = clusterId;
		return this;
	}

	public CacheBuilder withSSL() {
		this.useSSL = true;
		return this;
	}

	public CacheBuilder withServerAddress(ServerAddress serverAddress) {
		if (this.serverAdresses == null)
			throw new UnsupportedOperationException("Server Adresses can only be set when using a remote cache");
		this.serverAdresses.add(serverAddress);
		return this;
	}

	public CacheBuilder withTTL(long ttl, TimeUnit unit) {
		Objects.nonNull(unit);
		this.ttl = ttl;
		this.ttlUnit = unit;

		return this;
	}

	public CacheBuilder withLocalCache() {
		this.useLocalCache = true;
		return this;
	}

	public CacheBuilder withCustomSerializer(Serializer serializer){
		this.serializer = serializer;
		return this;
	}

	private ObjectCacheClientConfig buildConfig() {
		ObjectCacheClientConfig config = null;
		
		switch (this.mode) {
		case LOCAL_JVM_CACHE:
			if (ttlUnit != null && ttl != 0)
				config = new ObjectCacheClientConfig(ttl, ttlUnit);
			else
				config = new ObjectCacheClientConfig();

			return config;

		case STAND_ALONE_REDIS_CACHE:
			if (serverAdresses == null || serverAdresses.isEmpty())
				throw new InvalidConfigException("Server Adresses not set");

			if (ttlUnit != null && ttl != 0)
				config = new ObjectCacheClientConfig(ttl, ttlUnit, serverAdresses.toArray(new ServerAddress[0]));
			else
				config = new ObjectCacheClientConfig(serverAdresses.toArray(new ServerAddress[0]));

			if(serializer!=null)
				config.useRedisCache().withCustomSerializer(serializer);

			return config;

		case AWS_ELASTICACHE:
			if (awsClient == null || cacheClusterId == null)
				throw new InvalidConfigException("Aws Config not set");

			if (ttlUnit != null && ttl != 0) {
				config = new ObjectCacheClientConfig(awsClient, cacheClusterId, useSSL, ttl, ttlUnit);
			} else {
				config = new ObjectCacheClientConfig(awsClient, cacheClusterId, useSSL);
			}

			if(serializer!=null)
				config.useRedisCache().withCustomSerializer(serializer);

			return config;

		default:
			throw new IllegalStateException("No Configuration Selected, verify the caching mode");
		}
	}

	public <T> DataCache<T> build() {
		ObjectCacheClientConfig config = buildConfig();
		if (this.useLocalCache)
			config.useRedisCache().withLocalCache();

		CachingClient<T> cachingClient = new CachingClient<>(config);
		if(this.cacheId!=null)
			return cachingClient.getCache(cacheId);
		return cachingClient.getCache();
		
	}

	public <T> DataCache<T> build(Function<String, T> cacheLoader) {
		ObjectCacheClientConfig config = buildConfig();
		if (this.useLocalCache)
			config.useRedisCache().withLocalCache();

		CachingClient<T> cachingClient = new CachingClient<>(config);
		if(this.cacheId!=null)
			return cachingClient.getCache(cacheLoader, cacheId);
		return cachingClient.getCache(cacheLoader);
	}
}
