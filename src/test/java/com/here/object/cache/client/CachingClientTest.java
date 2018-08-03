package com.here.object.cache.client;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.CacheNode;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersResult;
import com.amazonaws.services.elasticache.model.Endpoint;
import com.here.object.cache.config.ObjectCacheClientConfig;
import com.here.object.cache.config.redis.ServerAddress;
import com.here.object.cache.data.DataCache;
import com.here.object.cache.data.RedisCache;

import redis.embedded.RedisServer;


public class CachingClientTest {

	private static RedisServer redisServer;
	private static final int redisServerPort= 32768;
	
	@BeforeClass
	public static void setUp() throws Exception {
		redisServer= new RedisServer(redisServerPort);
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
		
		ObjectCacheClientConfig clientConfig = new ObjectCacheClientConfig(new ServerAddress("localhost", redisServerPort, false));
		CachingClient<Map<String, String>> cacheClient = new CachingClient<>(clientConfig);
		
		DataCache<Map<String, String>> cache = cacheClient.getCache();
		cache.store("map", testMap);
		
		//Delete from Local Cache to force fetching from remote cache
		RedisCache<Map<String, String>> redisCache= (RedisCache<Map<String, String>>) cache;
		Field localCacheField= RedisCache.class.getDeclaredField("localCache");
		localCacheField.setAccessible(true);
		DataCache<Map<String, String>> localCache= (DataCache<Map<String, String>>) localCacheField.get(redisCache);
		localCache.deleteIfPresent("map");
		
		testMap= cache.get("map");
		Assert.assertNotNull("Storage was unsuccessful, fetched empty result", testMap);
		
		Assert.assertEquals(2, testMap.size());
		
		cache.deleteIfPresent("map");
		testMap= cache.get("map");
		Assert.assertNull("Deletion was unsuccessful, fetched non-null result", testMap);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testLocalCacheWithinRemoteCache() throws Exception {
		String testValue=new String("TEST_OBJECT");
		String key="key";
		
		ObjectCacheClientConfig clientConfig = new ObjectCacheClientConfig(new ServerAddress("localhost", redisServerPort, false));
		CachingClient<String> cacheClient = new CachingClient<>(clientConfig);
		
		DataCache<String> cache = cacheClient.getCache();
		cache.store(key, testValue);
		
		boolean sameObject=testValue==cache.get(key);
		Assert.assertTrue(sameObject);
		
		//Delete from Local Cache to force fetching from remote cache
		RedisCache<String> redisCache= (RedisCache<String>) cache;
		Field localCacheField= RedisCache.class.getDeclaredField("localCache");
		localCacheField.setAccessible(true);
		DataCache<String> localCache= (DataCache<String>) localCacheField.get(redisCache);
		localCache.deleteIfPresent(key);
		
		sameObject=testValue==cache.get(key);
		Assert.assertFalse(sameObject);
		
		cache.deleteIfPresent(key);
		testValue=cache.get(key);
		Assert.assertNull("Deletion was unsuccessful, fetched non-null result", testValue);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAmazonElasticache() throws Exception {
		 String testValue=new String("TEST_OBJECT");
		 String key="key";
		
		 AmazonElastiCacheClient amazonElastiCacheClient = Mockito.mock(AmazonElastiCacheClient.class);
		 DescribeCacheClustersRequest sampleCacheOneLogical = new DescribeCacheClustersRequest().withCacheClusterId("sampleCacheOneLogical");
		 sampleCacheOneLogical.setShowCacheNodeInfo(true);
		 
		 Mockito.when(amazonElastiCacheClient.describeCacheClusters(sampleCacheOneLogical)).
         		 thenReturn(new DescribeCacheClustersResult().
         				 			withCacheClusters(new CacheCluster().
         				 			withCacheNodes(new CacheNode()
         				 					.withEndpoint(new Endpoint().withAddress("localhost")
         				 					.withPort(redisServerPort)))));
		 
		ObjectCacheClientConfig clientConfig = new ObjectCacheClientConfig(amazonElastiCacheClient, "sampleCacheOneLogical", false);
		CachingClient<String> cacheClient = new CachingClient<>(clientConfig);
		
		DataCache<String> cache = cacheClient.getCache();
		cache.store(key, testValue);
		
		boolean sameObject=testValue==cache.get(key);
		Assert.assertTrue(sameObject);
		
		//Delete from Local Cache to force fetching from remote cache
		RedisCache<String> redisCache= (RedisCache<String>) cache;
		Field localCacheField= RedisCache.class.getDeclaredField("localCache");
		localCacheField.setAccessible(true);
		DataCache<String> localCache= (DataCache<String>) localCacheField.get(redisCache);
		localCache.deleteIfPresent(key);
		
		sameObject=testValue==cache.get(key);
		Assert.assertFalse(sameObject);
		
		cache.deleteIfPresent(key);
		testValue=cache.get(key);
		Assert.assertNull("Deletion was unsuccessful, fetched non-null result", testValue);
		
	}
}
