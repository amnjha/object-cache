package com.here.object.cache.config;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.CacheNode;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersResult;
import com.here.object.cache.config.local.LocalCacheConfig;
import com.here.object.cache.config.redis.RedisCacheConfig;
import com.here.object.cache.config.redis.ServerAddress;

/**
 * 
 * @author amajha
 *
 */
public class ObjectCacheClientConfig {
	private final CachingMode cachingMode;
	private final CacheConfig cacheConfig;

	public ObjectCacheClientConfig() {
		cachingMode = CachingMode.LOCAL_JVM_CACHE;
		cacheConfig= new LocalCacheConfig(7, TimeUnit.DAYS);
	}

	/**
	 *
	 * @param expiration
	 * @param timeUnit
	 */
	public ObjectCacheClientConfig(long expiration, TimeUnit timeUnit) {
		cachingMode = CachingMode.LOCAL_JVM_CACHE;
		cacheConfig= new LocalCacheConfig(expiration, timeUnit);
	}

	/**
	 *
	 * @param addresses
	 */
	public ObjectCacheClientConfig(ServerAddress... addresses) {
		cachingMode = CachingMode.STAND_ALONE_REDIS_CACHE;
		cacheConfig = new RedisCacheConfig(CachingMode.STAND_ALONE_REDIS_CACHE, addresses);
	}

	/**
	 *
	 * @return
	 */
	public RedisCacheConfig useRedisCache(){
		if(this.cacheConfig instanceof RedisCacheConfig){
			return (RedisCacheConfig) this.cacheConfig;
		}else{
			throw new RuntimeException("Redis configuration not available, current caching mode: "+ this.cachingMode);
		}
	}

	/**
	 *
	 * @param client
	 * @param cacheClusterId
	 * @param useSSL
	 */
	public ObjectCacheClientConfig(AmazonElastiCacheClient client, String cacheClusterId, boolean useSSL) {
		cachingMode = CachingMode.AWS_ELASTICACHE;

		DescribeCacheClustersRequest dccRequest = new DescribeCacheClustersRequest().withCacheClusterId(cacheClusterId);
		dccRequest.setShowCacheNodeInfo(true);
		System.out.println("Describe Cache cluster for cluster id: "+cacheClusterId);
		DescribeCacheClustersResult clusterResult = client.describeCacheClusters(dccRequest);
		List<CacheCluster> cacheClusters = clusterResult.getCacheClusters();

		CacheCluster cacheCluster = cacheClusters.get(0);
		ServerAddress[] addresses;
		if (cacheCluster != null)
		{	
			addresses = cacheCluster.getCacheNodes().stream().map(CacheNode::getEndpoint)
					.map(e -> new ServerAddress(e.getAddress(), e.getPort(), useSSL)).collect(Collectors.toList())
					.toArray(new ServerAddress[] {});
			cacheConfig= new RedisCacheConfig(CachingMode.AWS_ELASTICACHE, addresses);
		}
		else {
			cacheConfig=null;
		}
	}

	/**
	 * @return the cachingMode
	 */
	public CachingMode getCachingMode() {
		return cachingMode;
	}

	/**
	 * @return the cacheConfig
	 */
	public CacheConfig getCacheConfig() {
		return cacheConfig;
	}
	
	
	
}
