package com.here.object.cache.data;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.redisson.Redisson;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBinaryStream;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import com.here.object.cache.config.local.LocalCacheConfig;
import com.here.object.cache.config.redis.RedisCacheConfig;
import com.here.object.cache.config.redis.RedisCacheConfig.RedisConnectionType;
import com.here.object.cache.data.collections.CacheList;
import com.here.object.cache.data.collections.CacheSet;
import com.here.object.cache.exceptions.NonUniqueKeyException;
import com.here.object.cache.exceptions.ObjectNotSerialzableException;
import com.here.object.cache.serializer.ByteSerializer;

/**
 * 
 * @author amajha
 *
 * @param <T> The datatype that can be held within this cache
 */
public class RedisCache<T> implements DataCache<T> {

	private String cacheId;
	private LocalCache<T> localCache;
	private RedisCacheConfig cacheConfig;
	private RedissonClient client;
	private Config redissonConfig;
	private static Map<String, RedissonClient> clientHolder= new HashMap<>();
	private static final String SHARED_COUNTER="SHARED_COUNTER";
	private final String CACHE_KEY_APPENDER;
	private long timeToLive =0;
	private Function<String,T> valueLoader;
	
	/**
	 * @param cacheConfig
	 */
	public RedisCache(RedisCacheConfig cacheConfig) {
		super();
		this.cacheConfig = cacheConfig;
		redissonConfig= generateRedissonConfig();
		client= buildRedissonClient();

		this.cacheId= cacheConfig.getCacheId();
		if(cacheConfig.getCacheId()==null)
			this.cacheId = UUID.randomUUID().toString();

		if(this.cacheConfig.isEnableLocalCaching())
			this.localCache= new LocalCache<>(new LocalCacheConfig(cacheConfig.getLocalCacheSize(), 7, TimeUnit.DAYS), this, this.cacheId);
		if(this.cacheConfig.getExpirationInMs()!=0){
			this.timeToLive= this.cacheConfig.getExpirationInMs();
		}

		CACHE_KEY_APPENDER = cacheId;
	}
	
	/**
	 * @param cacheConfig
	 * @param valueLoader
	 */
	public RedisCache(RedisCacheConfig cacheConfig, Function<String, T> valueLoader) {
		super();
		this.cacheConfig = cacheConfig;
		redissonConfig= generateRedissonConfig();
		client= buildRedissonClient();

		this.cacheId= cacheConfig.getCacheId();
		if(cacheConfig.getCacheId()==null)
			this.cacheId = UUID.randomUUID().toString();

		if(this.cacheConfig.isEnableLocalCaching())
			this.localCache= new LocalCache<>(new LocalCacheConfig(cacheConfig.getLocalCacheSize(), 7, TimeUnit.DAYS), this, valueLoader, this.getCacheId());
		else
			this.valueLoader = valueLoader;
		
		if(this.cacheConfig.getExpirationInMs()!=0){
			this.timeToLive= this.cacheConfig.getExpirationInMs();
		}

		CACHE_KEY_APPENDER = cacheId;
	}

	public String getCacheId() {
		return cacheId;
	}

	@Override
	public T store(String key, T t) {
		if(!(t instanceof Serializable))
			throw new ObjectNotSerialzableException("Non-Serializable objects cannot be stored on Redis");
		
		// Store in the local cache
		if(this.cacheConfig.isEnableLocalCaching())
			localCache.store(key, t);
		
		//Store in the remote cache
		RBinaryStream binStream= client.getBinaryStream(CACHE_KEY_APPENDER + key);
		if(binStream.isExists())
			throw new NonUniqueKeyException("Key : " + key +" already present in the cache, to replace the value, use DataCache::replace() instead.");
		
		if(timeToLive!=0)
			binStream.set(ByteSerializer.serialize((Serializable) t), timeToLive, TimeUnit.MILLISECONDS);
		else
			binStream.set(ByteSerializer.serialize((Serializable) t));
		return null;
	}

	@Override
	public T store(String key, T t, long timeToLive, TimeUnit timeUnit) {
		if(!(t instanceof Serializable))
			throw new ObjectNotSerialzableException("Non-Serializable objects cannot be stored on Redis");

		// Store in the local cache
		if(this.cacheConfig.isEnableLocalCaching())
			localCache.store(key, t);

		//Store in the remote cache
		RBinaryStream binStream= client.getBinaryStream(CACHE_KEY_APPENDER + key);
		if(binStream.isExists())
			throw new NonUniqueKeyException("Key : " + key + " already present in the cache, to replace the value, use DataCache::replace() instead.");

		binStream.set(ByteSerializer.serialize((Serializable) t), timeToLive, timeUnit);
		return null;
	}

	@Override
	public T get(String key) {
		
		//Check whether it exists in local cache
		if(this.cacheConfig.isEnableLocalCaching()) {
			T t = localCache.get(key);
			if (t != null) {
				//if found in local cache, validate if it still exists in the remote cache
				RBinaryStream binStream = client.getBinaryStream(CACHE_KEY_APPENDER + key);
				if (binStream.isExists())
					return t;
			}
		}
		
		// if not found, look in the remote cache
		RBinaryStream binStream= client.getBinaryStream(CACHE_KEY_APPENDER + key);
		if(binStream.isExists())
			return ByteSerializer.deserialize(binStream.get());
		
		// If Still not found, try to use the cache loader and load the remote cache before returning the value
		if(valueLoader!=null && !this.cacheConfig.isEnableLocalCaching()) {
			T t = valueLoader.apply(key);
			Optional.ofNullable(t).ifPresent(e->store(key, e));
			return t;
		}
		
		return null;
	}

	public T getFromRemote(String key){
		RBinaryStream binStream= client.getBinaryStream(CACHE_KEY_APPENDER + key);
		if(binStream.isExists())
			return ByteSerializer.deserialize(binStream.get());

		return null;
	}

	@Override
	public T replace(String key, T t) {
		if(!(t instanceof Serializable))
			throw new ObjectNotSerialzableException("Non-Serializable objects cannot be stored on Redis");
		
		//Replace in the local cache
		if(this.cacheConfig.isEnableLocalCaching())
			localCache.replace(key, t);
		
		//Replace in the remote cache
		RBinaryStream binStream= client.getBinaryStream(CACHE_KEY_APPENDER + key);

		if(timeToLive!=0)
			binStream.set(ByteSerializer.serialize((Serializable) t), timeToLive, TimeUnit.MILLISECONDS);
		else
			binStream.set(ByteSerializer.serialize((Serializable) t));
		
		return t;
	}

	@Override
	public T replace(String key, T t, long timeToLive, TimeUnit timeUnit) {
		if(!(t instanceof Serializable))
			throw new ObjectNotSerialzableException("Non-Serializable objects cannot be stored on Redis");

		//Replace in the local cache
		if(this.cacheConfig.isEnableLocalCaching())
			localCache.replace(key, t);

		//Replace in the remote cache
		RBinaryStream binStream= client.getBinaryStream(CACHE_KEY_APPENDER + key);

		binStream.set(ByteSerializer.serialize((Serializable) t), timeToLive, timeUnit);

		return t;
	}

	@Override
	public boolean deleteIfPresent(String key) {
		
		//delete from local cache
		if(this.cacheConfig.isEnableLocalCaching())
			localCache.deleteIfPresent(key);
		
		//delete from remote cache as well
		RBinaryStream binStream= client.getBinaryStream(CACHE_KEY_APPENDER + key);
		return binStream.delete();
	}
	
	@Override
	public AtomicCounter getSharedAtomicCounter(String counterName) {
		RAtomicLong counterValue = client.getAtomicLong(CACHE_KEY_APPENDER + counterName + RedisCache.SHARED_COUNTER);
		AtomicCounter counter = new AtomicCounter(counterValue);
		return counter;
	}

	/**
	 * Creates a new set if not already present on the cache and returns the new/existing set
	 *
	 * @param setName the name against which the set is stored in the cache
	 * @return List of T
	 */
	@Override
	public Set<T> getSet(String setName) {
		Set<T> set = new CacheSet<>(client.getSet(CACHE_KEY_APPENDER + setName));
		return set;
	}

	/**
	 * Creates a new list if not already present on the cache and returns the new/existing list
	 *
	 * @param listName the name against which the list is stored in the cache
	 * @return List of T
	 */
	@Override
	public List<T> getList(String listName) {
		List<T> list = new CacheList<>(client.getList(CACHE_KEY_APPENDER + listName));
		return list;
	}

	@Override
	public List<String> getAllKeys(){
		RKeys keys= client.getKeys();
		List<String> keyList = new ArrayList<>();
		List<String> actualKeys = new ArrayList<>();
		keys.getKeys().forEach(actualKeys::add);
		actualKeys.stream().map(val -> val.replace(CACHE_KEY_APPENDER, "")).forEach(keyList::add);
		return keyList;
	}

	@Override
	public List<String> getKeyListByPattern(String keyPattern){
		RKeys keys= client.getKeys();
		List<String> keyList = new ArrayList<>();
		keys.getKeysByPattern(keyPattern).forEach(keyList::add);
		return keyList;
	}

	@Override
	public long deleteByKeyPattern(String keyPattern){
		RKeys keys= client.getKeys();
		return keys.deleteByPattern(keyPattern);
	}

	@Override
	public long deleteByKeys(String...keys){
		RKeys key= client.getKeys();
		return key.delete(keys);
	}

	private Config generateRedissonConfig() {
		Config config= new Config();
		
		if(cacheConfig.getRedisConnectionType().equals(RedisConnectionType.SINGLE_SERVER)) {
			config.useSingleServer().setAddress(cacheConfig.getRedisServers().get(0));
			config.useSingleServer().setRetryAttempts(Integer.MAX_VALUE);
	        config.useSingleServer().setTimeout(10000);
	        config.useSingleServer().setConnectionPoolSize(50);
	        config.useSingleServer().setRetryInterval(2000);
		}
		else {
			config.useClusterServers().addNodeAddress((String[]) cacheConfig.getRedisServers().toArray());
			config.useClusterServers().setRetryAttempts(Integer.MAX_VALUE);
	        config.useClusterServers().setTimeout(10000);
	        config.useClusterServers().setMasterConnectionPoolSize(50);
	        config.useClusterServers().setRetryInterval(2000);
		}
        
		return config;
	}

	private RedissonClient buildRedissonClient() {
		String clientKey= this.cacheConfig.getRedisServers().stream().sorted().collect(Collectors.joining(";;"));
		RedissonClient client= clientHolder.get(clientKey);
		if(client!=null && client.getNodesGroup().pingAll()) 
			return client;
		else {
			client= Redisson.create(this.redissonConfig);
			clientHolder.put(clientKey, client);
			return client;
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if(localCache!=null)
			this.localCache.deleteCacheReference();

		client.shutdown();
	}
	
}
