package com.here.object.cache.data;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.redisson.Redisson;
import org.redisson.api.RBinaryStream;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import com.here.object.cache.config.local.LocalCacheConfig;
import com.here.object.cache.config.redis.RedisCacheConfig;
import com.here.object.cache.config.redis.RedisCacheConfig.RedisConnectionType;
import com.here.object.cache.exceptions.NonUniqueKeyException;
import com.here.object.cache.exceptions.ObjectNotSerialzableException;
import com.here.object.cache.serializer.ByteSerializer;

public class RedisCache<T> implements DataCache<T> {

	private LocalCache<T> localCache;
	private RedisCacheConfig cacheConfig;
	private RedissonClient client;
	private Config redissonConfig;
	
	/**
	 * @param cacheConfig
	 */
	public RedisCache(RedisCacheConfig cacheConfig) {
		super();
		this.cacheConfig = cacheConfig;
		redissonConfig= generateRedissonConfig();
		client= Redisson.create(redissonConfig);
		
		this.localCache= new LocalCache<>(new LocalCacheConfig(7, TimeUnit.DAYS));
	}

	@Override
	public T store(String key, T t) {
		if(!(t instanceof Serializable))
			throw new ObjectNotSerialzableException("Non-Serializable objects cannot be stored on Redis");
		
		// Store in the local cache
		localCache.store(key, t);
		
		//Store in the remote cache
		RBinaryStream binStream= client.getBinaryStream(key);
		if(binStream.isExists())
			throw new NonUniqueKeyException("Key : "+key +" already present in the cache, to replace the value, use DataCache::replace() instead.");
		
		binStream.set(ByteSerializer.serialize((Serializable) t));
		return null;
	}

	@Override
	public T get(String key) {
		
		//Check whether it exists in local cache
		T t = localCache.get(key);
		if(t!=null) {
			//if found in local cache, validate if it still exists in the remote cache
			RBinaryStream binStream= client.getBinaryStream(key);	
			if(binStream.isExists())
				return t;  
		}
		
		// if not found, look in the remote cache
		RBinaryStream binStream= client.getBinaryStream(key);	
		if(binStream.isExists())
			return ByteSerializer.deserizalize(binStream.get());
		
		return null;
	}

	@Override
	public T replace(String key, T t) {
		if(!(t instanceof Serializable))
			throw new ObjectNotSerialzableException("Non-Serializable objects cannot be stored on Redis");
		
		//Replace in the local cache
		localCache.replace(key, t);
		
		//Replace in the remote cache
		RBinaryStream binStream= client.getBinaryStream(key);
		binStream.set(ByteSerializer.serialize((Serializable) t));
		
		return t;
	}

	@Override
	public boolean deleteIfPresent(String key) {
		
		//delete from local cache
		localCache.deleteIfPresent(key);
		
		//delete from remote cache as well
		RBinaryStream binStream= client.getBinaryStream(key);
		return binStream.delete();
	}

	private Config generateRedissonConfig() {
		Config config= new Config();
		
		if(cacheConfig.getRedisConnectionType().equals(RedisConnectionType.SINGLE_SERVER))
			config.useSingleServer().setAddress(cacheConfig.getRedisServers().get(0));
		else
			config.useClusterServers().addNodeAddress((String[]) cacheConfig.getRedisServers().toArray());		
		
		return config;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		client.shutdown();
	}
	
	
}
