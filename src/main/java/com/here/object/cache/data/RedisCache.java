package com.here.object.cache.data;

import com.google.common.collect.Lists;
import com.here.object.cache.config.CachingMode;
import com.here.object.cache.config.local.LocalCacheConfig;
import com.here.object.cache.config.redis.RedisCacheConfig;
import com.here.object.cache.config.redis.ServerAddress;
import com.here.object.cache.exceptions.NonUniqueKeyException;
import com.here.object.cache.exceptions.ObjectNotSerialzableException;
import com.here.object.cache.serializer.Serializer;
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.codec.RedisCodec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @param <T> The datatype that can be held within this cache
 * @author amajha
 */
public class RedisCache<T> implements DataCache<T> {

	private static final String SHARED_COUNTER = "SHARED_COUNTER";
	private static final int FETCH_SIZE = 10;
	private final String CACHE_KEY_APPENDER;
	private String cacheId;
	private LocalCache<T> localCache;
	private RedisCacheConfig cacheConfig;
	private RedisCodec<String, T> redisCodec;
	private RedisClient client;
	private RedisClusterClient clusterClient;
	private RedisAdvancedClusterReactiveCommands<String, T> clusterReactiveCommands;
	private RedisReactiveCommands<String, T> redisReactiveCommands;
	private long timeToLive = 0;
	private Function<String, T> valueLoader;
	private Serializer serializer;

	/**
	 * @param cacheConfig
	 */
	public RedisCache(RedisCacheConfig cacheConfig) {
		super();
		this.cacheConfig = cacheConfig;
		buildRedisClient();
		this.serializer = cacheConfig.getSerializer();

		this.cacheId = cacheConfig.getCacheId();
		if (cacheConfig.getCacheId() == null)
			this.cacheId = UUID.randomUUID().toString();

		if (this.cacheConfig.isEnableLocalCaching())
			this.localCache = new LocalCache<>(new LocalCacheConfig(cacheConfig.getLocalCacheSize(), 7, TimeUnit.DAYS), this, this.cacheId);
		if (this.cacheConfig.getExpirationInMs() != 0) {
			this.timeToLive = this.cacheConfig.getExpirationInMs();
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
		buildRedisClient();

		this.cacheId = cacheConfig.getCacheId();
		if (cacheConfig.getCacheId() == null)
			this.cacheId = UUID.randomUUID().toString();

		if (this.cacheConfig.isEnableLocalCaching())
			this.localCache = new LocalCache<>(new LocalCacheConfig(cacheConfig.getLocalCacheSize(), 7, TimeUnit.DAYS), this, valueLoader, this.getCacheId());
		else
			this.valueLoader = valueLoader;

		if (this.cacheConfig.getExpirationInMs() != 0) {
			this.timeToLive = this.cacheConfig.getExpirationInMs();
		}

		CACHE_KEY_APPENDER = cacheId;
	}

	public String getCacheId() {
		return cacheId;
	}

	private void validateStore(String key, T t) {
		if (!(t instanceof Serializable))
			throw new ObjectNotSerialzableException("Non-Serializable objects cannot be stored on Redis");

		// Store in the local cache
		if (this.cacheConfig.isEnableLocalCaching())
			localCache.store(key, t);

		//Check in the remote cache
		if (validateRemoteExistence(key))
			throw new NonUniqueKeyException("Key : " + key + " already present in the cache, to replace the value, use DataCache::replace() instead.");
	}

	private void set(String key, T value) {
		set(key, value, null);
	}

	private void set(String key, T value, SetArgs setArgs) {
		if (CachingMode.CLUSTER_MODE_REDIS_CACHE.equals(this.cacheConfig.getCachingMode())) {
			if (setArgs != null)
				clusterReactiveCommands.set(CACHE_KEY_APPENDER + key, value, SetArgs.Builder.ex(TimeUnit.SECONDS.convert(timeToLive, TimeUnit.MILLISECONDS))).block();
			else
				clusterReactiveCommands.set(CACHE_KEY_APPENDER + key, value).block();
		} else {
			if (setArgs != null)
				redisReactiveCommands.set(CACHE_KEY_APPENDER + key, value, SetArgs.Builder.ex(TimeUnit.SECONDS.convert(timeToLive, TimeUnit.MILLISECONDS))).block();
			else
				redisReactiveCommands.set(CACHE_KEY_APPENDER + key, value).block();
		}
	}

	@Override
	public T store(String key, T t) {
		validateStore(key, t);

		if (timeToLive != 0)
			set(key, t, SetArgs.Builder.ex(TimeUnit.SECONDS.convert(timeToLive, TimeUnit.MILLISECONDS)));
		else
			set(key, t);
		return t;
	}


	@Override
	public T store(String key, T t, long timeToLive, TimeUnit timeUnit) {
		validateStore(key, t);
		set(key, t, SetArgs.Builder.ex(TimeUnit.SECONDS.convert(timeToLive, timeUnit)));
		return t;
	}

	@Override
	public boolean storeBatch(Map<String, T> dataToInsert, long timeout, TimeUnit timeUnit) {
		if (CachingMode.CLUSTER_MODE_REDIS_CACHE.equals(this.cacheConfig.getCachingMode())) {
			return storeBatchForCluster(dataToInsert, timeout, timeUnit);
		} else if (CachingMode.STAND_ALONE_REDIS_CACHE.equals(this.cacheConfig.getCachingMode())) {
			StatefulRedisConnection<String, T> connection = client.connect(this.redisCodec);
			RedisAsyncCommands<String, T> commands = connection.async();
			commands.setAutoFlushCommands(false);
			List<RedisFuture<?>> futures = Lists.newArrayList();
			dataToInsert.entrySet()
					.parallelStream()
					.forEach(entry -> {
						futures.add(commands.set(CACHE_KEY_APPENDER + entry.getKey(), entry.getValue()));
					});
			commands.flushCommands();
			boolean result = LettuceFutures.awaitAll(timeout, timeUnit, futures.toArray(new RedisFuture[futures.size()]));
			connection.close();
			return result;
		}

		throw new UnsupportedOperationException("Caching mode should be one of : STAND_ALONE_REDIS_CACHE / CLUSTER_MODE_REDIS_CACHE");
	}

	private boolean storeBatchForCluster(Map<String, T> dataToInsert, long timeout, TimeUnit timeUnit) {
		StatefulRedisClusterConnection<String, T> connection = clusterClient.connect(this.redisCodec);
		RedisAdvancedClusterAsyncCommands<String, T> commands = connection.async();
		commands.setAutoFlushCommands(false);

		// perform a series of independent calls
		List<RedisFuture<?>> futures = Lists.newArrayList();
		dataToInsert.entrySet()
				.parallelStream()
				.forEach(entry -> {
					commands.set(CACHE_KEY_APPENDER + entry.getKey(), entry.getValue());
				});

		// write all commands to the transport layer
		commands.flushCommands();

		// synchronization example: Wait until all futures complete
		boolean result = LettuceFutures.awaitAll(timeout, timeUnit, futures.toArray(new RedisFuture[futures.size()]));

		// later
		connection.close();
		return result;
	}

	private boolean validateRemoteExistence(String key) {
		Mono<Long> value;
		if (CachingMode.CLUSTER_MODE_REDIS_CACHE.equals(this.cacheConfig.getCachingMode()))
			value = clusterReactiveCommands.exists(CACHE_KEY_APPENDER + key);
		else
			value = redisReactiveCommands.exists(CACHE_KEY_APPENDER + key);
		Long keyCount = value.block();
		return keyCount != null && keyCount != 0;
	}

	@Override
	public T get(String key) {

		//Check whether it exists in local cache
		if (this.cacheConfig.isEnableLocalCaching()) {
			T t = localCache.get(key);
			//if found in local cache, validate if it still exists in the remote cache
			if (t != null && validateRemoteExistence(key)) {
				return t;
			}
		}

		// if not found, look in the remote cache
		T value = getFromRemote(key);
		if (value != null)
			return value;

		// If Still not found, try to use the cache loader and load the remote cache before returning the value
		if (valueLoader != null && !this.cacheConfig.isEnableLocalCaching()) {
			T t = valueLoader.apply(key);
			Optional.ofNullable(t).ifPresent(e -> store(key, e));
			return t;
		}

		return null;
	}


	public T getFromRemote(String key) {
		if (CachingMode.CLUSTER_MODE_REDIS_CACHE.equals(this.cacheConfig.getCachingMode())) {
			return clusterReactiveCommands.get(CACHE_KEY_APPENDER + key).block();
		} else {
			return redisReactiveCommands.get(CACHE_KEY_APPENDER + key).block();
		}
	}

	@Override
	public boolean replaceBatch(Map<String, T> dataToInsert, long timeout, TimeUnit timeUnit) {
		return storeBatch(dataToInsert, timeout, timeUnit);
	}

	@Override
	public T replace(String key, T t) {
		if (!(t instanceof Serializable))
			throw new ObjectNotSerialzableException("Non-Serializable objects cannot be stored on Redis");

		//Replace in the local cache
		if (this.cacheConfig.isEnableLocalCaching())
			localCache.replace(key, t);

		if (timeToLive != 0)
			set(key, t, SetArgs.Builder.ex(TimeUnit.SECONDS.convert(timeToLive, TimeUnit.MILLISECONDS)));
		else
			set(key, t);

		return t;
	}

	@Override
	public T replace(String key, T t, long timeToLive, TimeUnit timeUnit) {
		if (!(t instanceof Serializable))
			throw new ObjectNotSerialzableException("Non-Serializable objects cannot be stored on Redis");

		//Replace in the local cache
		if (this.cacheConfig.isEnableLocalCaching())
			localCache.replace(key, t);

		//Replace in the remote cache
		set(key, t, SetArgs.Builder.ex(TimeUnit.SECONDS.convert(timeToLive, timeUnit)));
		return t;
	}

	@Override
	public boolean deleteIfPresent(String key) {

		//delete from local cache
		if (this.cacheConfig.isEnableLocalCaching())
			localCache.deleteIfPresent(key);

		//delete from remote cache as well
		Long delResponse = deleteByKeys(key);
		return delResponse != null && delResponse != 0;
	}

	@Override
	public Set<String> getAllKeys() {
		return getKeyListByPattern("*");
	}


	@Override
	public Set<String> getKeyListByPattern(String keyPattern) {
		Mono<KeyScanCursor<String>> scan;
		if (CachingMode.CLUSTER_MODE_REDIS_CACHE.equals(this.cacheConfig.getCachingMode()))
			scan = clusterReactiveCommands.scan(ScanCursor.INITIAL, ScanArgs.Builder.limit(10000000).match(CACHE_KEY_APPENDER + keyPattern + "*"));
		else
			scan = redisReactiveCommands.scan(ScanCursor.INITIAL, ScanArgs.Builder.limit(10000000).match(CACHE_KEY_APPENDER + keyPattern + "*"));
		Set<String> keys = scan.block().getKeys().stream().map(e -> e.replace(CACHE_KEY_APPENDER, "")).collect(Collectors.toSet());
		return keys;
	}

	@Override
	public ScanResult scanAllKeys(int limit) {
		return ScanResult.getInitial( CACHE_KEY_APPENDER + "*", this, limit);
	}

	@Override
	public ScanResult scanKeysByPattern(String keyPattern, int limit) {
		return ScanResult.getInitial(CACHE_KEY_APPENDER + keyPattern + "*", this, limit);
	}

	private ScanResult scanKeyListByPattern(ScanCursor scanCursor, String keyPattern, int limit) {
		Mono<KeyScanCursor<String>> scan;
		if (CachingMode.CLUSTER_MODE_REDIS_CACHE.equals(this.cacheConfig.getCachingMode()))
			scan = clusterReactiveCommands.scan(scanCursor, ScanArgs.Builder.limit(limit).match(keyPattern));
		else
			scan = redisReactiveCommands.scan(scanCursor, ScanArgs.Builder.limit(limit).match(keyPattern));

		KeyScanCursor<String> keyScanCursor = scan.block();
		List<String> keys = keyScanCursor.getKeys();

		Set<String> keySet;
		keySet = keys.stream().map(e -> e.replaceFirst(CACHE_KEY_APPENDER, "")).collect(Collectors.toSet());
		return new ScanResult(keyScanCursor, keySet, keyPattern, this, limit);
	}

	/**
	 * Delete multiple objects by a key pattern.
	 * <p>
	 * Method executes in <b>NON atomic way</b> in cluster mode due to lua script limitations.
	 * <p>
	 * Supported glob-style patterns:
	 * h?llo subscribes to hello, hallo and hxllo
	 * h*llo subscribes to hllo and heeeello
	 * h[ae]llo subscribes to hello and hallo, but not hillo
	 *
	 * @param keyPattern - match pattern
	 * @return number of removed keys
	 */
	@Override
	public long deleteByKeyPattern(String keyPattern) {
		Flux<String> keys;
		if (CachingMode.CLUSTER_MODE_REDIS_CACHE.equals(this.cacheConfig.getCachingMode()))
			keys = clusterReactiveCommands.keys(cacheId + keyPattern + "*");
		else
			keys = redisReactiveCommands.keys(cacheId + keyPattern + "*");

		List<String> keyList = keys.collectList().block();
		return deleteByKeys(keyList.toArray(new String[0]));
	}

	@Override
	public long deleteByKeys(String... keys) {
		String[] keysUpdated = new String[keys.length];
		for (int i = 0; i < keys.length; i++) {
			keysUpdated[i] = CACHE_KEY_APPENDER + keys[i];
		}

		if (CachingMode.CLUSTER_MODE_REDIS_CACHE.equals(this.cacheConfig.getCachingMode()))
			return clusterReactiveCommands.del(keysUpdated).block();
		else
			return redisReactiveCommands.del(keysUpdated).block();
	}

	@Override
	public Mono<Long> deleteByKeysAsync(String... keys) {
		if (CachingMode.CLUSTER_MODE_REDIS_CACHE.equals(this.cacheConfig.getCachingMode()))
			return clusterReactiveCommands.del(keys);
		else
			return redisReactiveCommands.del(keys);
	}

	/**
	 * Delete all keys of all existing databases in background without blocking server.
	 * Requires Redis 4.0+
	 *
	 * @return
	 */
	@Override
	public Mono<String> purgeCacheAsync() {
		if (this.cacheConfig.isEnableLocalCaching()) {
			this.localCache.purgeCache();
		}

		if (CachingMode.CLUSTER_MODE_REDIS_CACHE.equals(this.cacheConfig.getCachingMode()))
			return clusterReactiveCommands.flushall();
		else
			return redisReactiveCommands.flushall();
	}

	/**
	 * Delete all keys of all existing databases
	 */
	@Override
	public void purgeCache() {
		if (this.cacheConfig.isEnableLocalCaching()) {
			this.localCache.purgeCache();
		}

		if (CachingMode.CLUSTER_MODE_REDIS_CACHE.equals(this.cacheConfig.getCachingMode()))
			clusterReactiveCommands.flushall().block();
		else
			redisReactiveCommands.flushall().block();
	}

	private RedisCodec<String, T> buildRedisCodec() {
		this.redisCodec = new RedisCodec<String, T>() {
			@Override
			public String decodeKey(ByteBuffer bytes) {
				byte[] arr = new byte[bytes.remaining()];
				bytes.get(arr);
				return new String(arr);
			}

			@Override
			public T decodeValue(ByteBuffer bytes) {
				byte[] arr = new byte[bytes.remaining()];
				bytes.get(arr);
				return serializer.deserialize(arr);
			}

			@Override
			public ByteBuffer encodeKey(String key) {
				return ByteBuffer.wrap(key.getBytes());
			}

			@Override
			public ByteBuffer encodeValue(T value) {
				return ByteBuffer.wrap(serializer.serialize((Serializable) value));
			}
		};
		return this.redisCodec;
	}

	private void buildRedisClient() {
		buildRedisCodec();
		if (CachingMode.CLUSTER_MODE_REDIS_CACHE.equals(this.cacheConfig.getCachingMode())) {
			List<RedisURI> redisURIS = cacheConfig.getRedisServers().stream().map(ServerAddress::getRedisURI).collect(Collectors.toList());
			clusterClient = RedisClusterClient.create(redisURIS);
			clusterClient.setOptions(ClusterClientOptions.builder().autoReconnect(true).build());
			clusterClient.setDefaultTimeout(Duration.of(30, ChronoUnit.MINUTES));
			clusterClient.setOptions(
					ClusterClientOptions.builder()
							.autoReconnect(true).build()
			);
			StatefulRedisClusterConnection<String, T> connection = clusterClient.connect(this.redisCodec);
			clusterReactiveCommands = connection.reactive();

		} else {
			RedisURI redisURI = cacheConfig.getRedisServers().get(0).getRedisURI();
			client = RedisClient.create(redisURI);
			client.setOptions(ClusterClientOptions.builder().autoReconnect(true).build());
			client.setDefaultTimeout(Duration.of(30, ChronoUnit.MINUTES));
			client.setOptions(ClientOptions.builder().autoReconnect(true).build());
			StatefulRedisConnection<String, T> connection = client.connect(this.redisCodec);
			redisReactiveCommands = connection.reactive();
		}
	}

	/**
	 * Closes all connections to the Redis Cache
	 */
	public void closeClient() {
		if (localCache != null)
			this.localCache.deleteCacheReference();

		client.shutdown();
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		closeClient();
	}

	public static class ScanResult {
		private ScanCursor keyScanCursor;
		private Set<String> keys;
		private RedisCache redisCache;
		private int limit;
		private String pattern;

		ScanResult(ScanCursor keyScanCursor, Set<String> keys, RedisCache redisCache, int limit) {
			this.keys = keys;
			this.keyScanCursor = keyScanCursor;
			this.redisCache = redisCache;
			this.limit = limit;
			this.pattern = "*";
		}

		ScanResult(ScanCursor keyScanCursor, Set<String> keys, String pattern, RedisCache redisCache, int limit) {
			this.keys = keys;
			this.keyScanCursor = keyScanCursor;
			this.redisCache = redisCache;
			this.limit = limit;
			this.pattern = pattern;
		}

		static ScanResult getInitial(String pattern, RedisCache redisCache, int limit) {
			return new ScanResult(ScanCursor.INITIAL, null, pattern, redisCache, limit);
		}


		public Set<String> getKeys() {
			return keys;
		}

		public ScanResult getNext() {
			if (!keyScanCursor.isFinished()) {
				return redisCache.scanKeyListByPattern(this.keyScanCursor, this.pattern, this.limit);
			} else {
				return new ScanResult(this.keyScanCursor, null, null, limit);
			}
		}

		public boolean hasNext() {
			return !keyScanCursor.isFinished();
		}
	}

}
