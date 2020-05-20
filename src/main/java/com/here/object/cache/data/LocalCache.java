package com.here.object.cache.data;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.here.object.cache.config.local.LocalCacheConfig;
import com.here.object.cache.exceptions.NonUniqueKeyException;
import reactor.core.publisher.Mono;

/**
 * 
 * @author amajha
 *
 * @param <T> The datatype that can be held by this class
 */
public class LocalCache<T>  implements DataCache<T>{
	private LocalCacheConfig cacheConfig;
	private LoadingCache<String, T> localCache;
	private Cache<String, Collection<T>> collectionLocalCache;

	private RedisCache<T> remoteCache;
	private Function<String, T> valueSupplier;

	private String cacheId;

	private static HashMap<String, LocalCache<?>> cacheMap= new HashMap<>();

	/**
	 *
	 * @param cacheConfig
	 */
	public LocalCache(LocalCacheConfig cacheConfig) {
		super();
		this.cacheConfig = cacheConfig;
		this.localCache = configureLocalCache();
		this.collectionLocalCache=configureCollectionCache();
		this.cacheId = UUID.randomUUID().toString();
		LocalCache.cacheMap.put(cacheId, this);
	}

	/**
	 *
	 * @param cacheConfig
	 * @param cacheId
	 */
	public LocalCache(LocalCacheConfig cacheConfig, String cacheId) {
		super();
		this.cacheConfig = cacheConfig;
		this.localCache = configureLocalCache();
		this.collectionLocalCache=configureCollectionCache();
		this.cacheId = cacheId;
		LocalCache.cacheMap.put(cacheId, this);
	}

	/**
	 *
	 * @param cacheConfig
	 * @param valueSupplier
	 */
	public LocalCache(LocalCacheConfig cacheConfig, Function<String,T> valueSupplier) {
		super();
		this.cacheConfig = cacheConfig;
		this.localCache = configureLocalCache();
		this.collectionLocalCache=configureCollectionCache();
		this.valueSupplier = valueSupplier;
		this.cacheId = UUID.randomUUID().toString();
		LocalCache.cacheMap.put(cacheId, this);
	}

	/**
	 *
	 * @param cacheConfig
	 * @param valueSupplier
	 * @param cacheId
	 */
	public LocalCache(LocalCacheConfig cacheConfig, Function<String,T> valueSupplier, String cacheId) {
		super();
		this.cacheConfig = cacheConfig;
		this.localCache = configureLocalCache();
		this.collectionLocalCache=configureCollectionCache();
		this.valueSupplier = valueSupplier;
		this.cacheId = cacheId;
		LocalCache.cacheMap.put(cacheId, this);
	}


	/**
	 *
	 * @param cacheConfig
	 * @param redisCache
	 */
	protected LocalCache(LocalCacheConfig cacheConfig, RedisCache<T> redisCache) {
		super();
		this.cacheConfig = cacheConfig;
		this.localCache = configureLocalCache();
		this.collectionLocalCache=configureCollectionCache();
		this.remoteCache = redisCache;
		this.cacheId = UUID.randomUUID().toString();
		LocalCache.cacheMap.put(cacheId, this);
	}

	/**
	 *
	 * @param cacheConfig
	 * @param redisCache
	 */
	protected LocalCache(LocalCacheConfig cacheConfig, RedisCache<T> redisCache, String cacheId) {
		super();
		this.cacheConfig = cacheConfig;
		this.localCache = configureLocalCache();
		this.collectionLocalCache=configureCollectionCache();
		this.remoteCache = redisCache;
		this.cacheId = cacheId;
		if(cacheId == null)
			this.cacheId = UUID.randomUUID().toString();
		LocalCache.cacheMap.put(cacheId, this);
	}


	/**
	 *
	 * @param cacheConfig
	 * @param redisCache
	 * @param valueSupplier
	 */
	protected LocalCache(LocalCacheConfig cacheConfig, RedisCache<T> redisCache, Function<String, T> valueSupplier) {
		super();
		this.cacheConfig = cacheConfig;
		this.localCache = configureLocalCache();
		this.collectionLocalCache=configureCollectionCache();
		this.remoteCache = redisCache;
		this.valueSupplier = valueSupplier;
		this.cacheId = UUID.randomUUID().toString();
		LocalCache.cacheMap.put(cacheId, this);
	}

	/**
	 *
	 * @param cacheConfig
	 * @param redisCache
	 * @param valueSupplier
	 * @param cacheId
	 */
	protected LocalCache(LocalCacheConfig cacheConfig, RedisCache<T> redisCache, Function<String, T> valueSupplier, String cacheId) {
		super();
		this.cacheConfig = cacheConfig;
		this.localCache = configureLocalCache();
		this.collectionLocalCache=configureCollectionCache();
		this.remoteCache = redisCache;
		this.valueSupplier = valueSupplier;
		this.cacheId = cacheId;
		if(cacheId == null)
			this.cacheId = UUID.randomUUID().toString();
		LocalCache.cacheMap.put(cacheId, this);
	}

	private LoadingCache<String, T> configureLocalCache() {

		CacheLoader<String, T> cacheLoader = new CacheLoader<String, T>() {
			@Override
			public T load(String key) throws Exception {
				
				if (remoteCache != null) {
					return remoteCache.getFromRemote(key);
				}
				
				if(valueSupplier!=null) {
					T t = valueSupplier.apply(key);
					Optional.ofNullable(t).ifPresent(e->{
						if(remoteCache!=null)
							remoteCache.store(key, e);
					});
					return t;
				}
				
				return null;
			}
		};
		
		return CacheBuilder.newBuilder()
				.expireAfterWrite(cacheConfig.getExpirationInMs(), TimeUnit.MILLISECONDS)
				.maximumSize(cacheConfig.getCacheSize())
				.build(cacheLoader);
	}
	
	private Cache<String, Collection<T>> configureCollectionCache(){
		return CacheBuilder.
				newBuilder().
				expireAfterWrite(cacheConfig.getExpirationInMs(), TimeUnit.MILLISECONDS).
				maximumSize(cacheConfig.getCacheSize()).
				build();
	}

	public void deleteCacheReference() {
		LocalCache.cacheMap.remove(this.cacheId);
	}

	public static <T> LocalCache<T> getCacheById(String cacheId){
		return (LocalCache<T>) cacheMap.get(cacheId);
	}

	@Override
	public T store(String key, T t) {
		if(!Objects.isNull(localCache.getIfPresent(key)))
			throw new NonUniqueKeyException("Key : "+key +" already present in the cache, to replace the value, use DataCache::replace() instead.");
		
		localCache.put(key, t);
		return t;
	}


	@Override
	public T get(String key) {
		try {
			return localCache.getUnchecked(key);
		}catch (CacheLoader.InvalidCacheLoadException e){
			return null;
		}
	}

	@Override
	public T replace(String key, T t) {
		localCache.put(key, t);
		return t;
	}


	@Override
	public boolean deleteIfPresent(String key) {
		T t= localCache.getIfPresent(key);
		localCache.invalidate(key);
		return t!=null;
	}

	@Override
	public List<T> getList(String listName) {
		Collection<T> coll = collectionLocalCache.getIfPresent(listName);
		if(coll ==null) {
			List<T> t= new CopyOnWriteArrayList<>();
			collectionLocalCache.put(listName, t);
			return t;
		}

		if(!(coll instanceof List)) {
			throw new RuntimeException("given key is not a list");
		}else{
			return (List<T>)coll;
		}
	}

	@Override
	public List<String> getAllKeys() {
		List<String> keys = new ArrayList<>();
		keys.addAll(localCache.asMap().keySet());
		keys.addAll(collectionLocalCache.asMap().keySet());
		return keys;
	}

	@Override
	public List<String> getKeyListByPattern(String keyPattern) {
		throw new RuntimeException("Method not supported on local cache");
	}

	@Override
	public long deleteByKeyPattern(String keyPattern) {
		return 0;
	}


	@Override
	public long deleteByKeys(String... keys) {
		long deleteCount=0;
		for(String key: keys){
			T t= localCache.getIfPresent(key);
			if(t!=null){
				localCache.invalidate(key);
				deleteCount++;
			}else if(collectionLocalCache.getIfPresent(key)!=null){
				collectionLocalCache.invalidate(key);
				deleteCount++;
			}
		}
		return deleteCount;
	}

	@Override
	public Mono<Long> deleteByKeysAsync(String... keys) {
		throw new AbstractMethodError("Method Unavailable in Local Cache");
	}

	@Override
	public Set<T> getSet(String setName) {
		Collection<T> coll = collectionLocalCache.getIfPresent(setName);
		if(coll==null) {
			Set<T> t= new CopyOnWriteArraySet<>();
			collectionLocalCache.put(setName, t);
			return t;
		}
		
		if(!(coll instanceof Set)) {
			throw new RuntimeException("given key is not a set");
		}else{
			return (Set<T>) coll;
		}
	}


	@Override
	public void purgeCache(){
		localCache.invalidateAll();
		localCache.cleanUp();
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		purgeCache();
	}
	
	
}
