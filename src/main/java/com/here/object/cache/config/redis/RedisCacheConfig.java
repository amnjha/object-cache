package com.here.object.cache.config.redis;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;

import com.here.object.cache.config.CacheConfig;
import com.here.object.cache.exceptions.InvalidConfigException;

public class RedisCacheConfig implements CacheConfig{
	
	public enum RedisConnectionType{
		SINGLE_SERVER,
		CLUSTER_CONNECTION;
	}
	
	private final List<String> redisServers;
	private final RedisConnectionType redisConnectionType;
	
	public RedisCacheConfig(ServerAddress...servers) {
		int serverCount= servers.length;
		if(serverCount<1) 
			throw new InvalidConfigException("Atleast one server is required while using the redis caching mode");
		
		redisServers = Arrays.stream(servers).map(ServerAddress::getConnectionString).collect(toList());
		redisConnectionType= serverCount>1?RedisConnectionType.CLUSTER_CONNECTION:RedisConnectionType.SINGLE_SERVER;
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
	
	
}
