package com.here.object.cache.config;

import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.here.object.cache.config.redis.RedisCacheConfig;
import com.here.object.cache.config.redis.ServerAddress;

public class ObjectCacheConfig {
		private CachingMode cachingMode;
		private CacheConfig cacheConfig;
		
		public ObjectCacheConfig() {
			cachingMode = CachingMode.LOCAL_JVM_CACHE;
		}
		
		public ObjectCacheConfig(ServerAddress...addresses) {
			cachingMode= CachingMode.STAND_ALONE_REDIS_CACHE;
			cacheConfig= new RedisCacheConfig(addresses);
		}
		
		public ObjectCacheConfig(AmazonElastiCache elastiCache) {
			cachingMode= CachingMode.AWS_ELASTICACHE;
			//elastiCache.nodes
		}
}
