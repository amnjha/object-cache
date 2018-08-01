package com.here.object.cache.client;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.here.object.cache.config.ObjectCacheClientConfig;
import com.here.object.cache.config.redis.ServerAddress;
import com.here.object.cache.data.DataCache;
import com.here.object.cache.data.LocalCache;
import com.here.object.cache.data.RedisCache;

import redis.embedded.RedisServer;


public class CachingClientTest {

	private static RedisServer redisServer;
	
	@BeforeClass
	public static void setUp() throws Exception {
		redisServer= new RedisServer(6379);
		redisServer.start();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		redisServer.stop();
		redisServer=null;
	}
	
	@Test
	public void localCacheTest() {
		Map<String, String> testMap= new HashMap<>();
		testMap.put("key", "value");
		testMap.put("key1", "value1");
		
		ObjectCacheClientConfig clientConfig = new ObjectCacheClientConfig();
		CachingClient<Map<String, String>> cacheClient = new CachingClient<>(clientConfig);
		
		DataCache<Map<String, String>> cache = cacheClient.getCache();
		cache.store("map", testMap);
		
		testMap= cache.get("map");
		Assert.assertNotNull("Storage was unsuccessful, fetched empty result", testMap);
		
		Assert.assertEquals(2, testMap.size());
		
		cache.deleteIfPresent("map");
		testMap= cache.get("map");
		Assert.assertNull("Deletion was unsuccessful, fetched non-null result", testMap);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void remoteCacheTest() throws Exception {
		Map<String, String> testMap= new HashMap<>();
		testMap.put("key", "value");
		testMap.put("key1", "value1");
		
		ObjectCacheClientConfig clientConfig = new ObjectCacheClientConfig(new ServerAddress("localhost", 6379, false));
		CachingClient<Map<String, String>> cacheClient = new CachingClient<>(clientConfig);
		
		DataCache<Map<String, String>> cache = cacheClient.getCache();
		cache.store("map", testMap);
		
		//Delete from Local Cache to force fetching from remote cache
		RedisCache<Map<String, String>> redisCache= (RedisCache<Map<String, String>>) cache;
		Field localCacheField= RedisCache.class.getDeclaredField("localCache");
		localCacheField.setAccessible(true);
		LocalCache<Map<String, String>> localCache= (LocalCache<Map<String, String>>) localCacheField.get(redisCache);
		localCache.deleteIfPresent("map");
		
		testMap= cache.get("map");
		Assert.assertNotNull("Storage was unsuccessful, fetched empty result", testMap);
		
		Assert.assertEquals(2, testMap.size());
		
		cache.deleteIfPresent("map");
		testMap= cache.get("map");
		Assert.assertNull("Deletion was unsuccessful, fetched non-null result", testMap);
	}

}
