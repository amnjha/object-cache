package com.here.object.cache.client;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.*;
import org.mockito.Mockito;

import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.CacheNode;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersResult;
import com.amazonaws.services.elasticache.model.Endpoint;
import com.here.object.cache.builder.CacheBuilder;
import com.here.object.cache.config.CachingMode;
import com.here.object.cache.config.ObjectCacheClientConfig;
import com.here.object.cache.config.redis.ServerAddress;
import com.here.object.cache.data.DataCache;
import com.here.object.cache.data.RedisCache;

import redis.embedded.RedisServer;


public class CachingClientTest {

	private static RedisServer redisServer;
	private static final int redisServerPort= 32769;
	
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
		clientConfig.useRedisCache().withLocalCache();

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

	@Test
	public void remoteCacheIteratorTest() throws Exception {
		ObjectCacheClientConfig clientConfig = new ObjectCacheClientConfig(new ServerAddress("localhost", redisServerPort, false));
		clientConfig.useRedisCache();

		CachingClient<String> cacheClient = new CachingClient<>(clientConfig);
		DataCache<String> cache = cacheClient.getCache();
		cache.store("test1", "test_1");
		cache.store("test2", "test_2");
		List<String> expectedList = Arrays.asList("test1","test2");
		final Iterator<String> keyIterator = cache.getKeyIterator();
		while(keyIterator.hasNext()){
			Assert.assertTrue(expectedList.contains(keyIterator.next()));
		}
		cache.deleteIfPresent("test1");
		cache.deleteIfPresent("test2");
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testLocalCacheWithinRemoteCache() throws Exception {
		String testValue=new String("TEST_OBJECT");
		String key="key";
		
		ObjectCacheClientConfig clientConfig = new ObjectCacheClientConfig(new ServerAddress("localhost", redisServerPort, false));
		clientConfig.useRedisCache().withLocalCache();

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
		clientConfig.useRedisCache().withLocalCache();

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
		
		sameObject = testValue == cache.get(key);
		Assert.assertFalse(sameObject); // Check the reference is different

		Assert.assertEquals(testValue,cache.get(key));
		
		cache.deleteIfPresent(key);
		testValue=cache.get(key);
		Assert.assertNull("Deletion was unsuccessful, fetched non-null result", testValue);
		
	}

	@Test
	public void remoteCacheTTLTest() throws Exception {
		Map<String, String> testMap= new HashMap<>();
		testMap.put("key", "value");
		testMap.put("key1", "value1");

		ObjectCacheClientConfig clientConfig = new ObjectCacheClientConfig(2, TimeUnit.SECONDS, new ServerAddress("localhost", redisServerPort, false));

		CachingClient<Map<String, String>> cacheClient = new CachingClient<>(clientConfig);

		DataCache<Map<String, String>> cache = cacheClient.getCache();
		cache.store("map", testMap);

		testMap= cache.get("map");
		Assert.assertNotNull("Storage was unsuccessful, fetched empty result", testMap);

		Assert.assertEquals(2, testMap.size());

		TimeUnit.SECONDS.sleep(2);

		testMap= cache.get("map");
		Assert.assertNull("Object not expired after TTL, fetched non-null result", testMap);
	}
	
	@Test
	public void cacheLoaderTest() {
		Function<String, String> valueSupplier = e -> e ;

		DataCache<String> cache = CacheBuilder.newBuilder().withCachingMode(CachingMode.LOCAL_JVM_CACHE).build(valueSupplier);
		
		String value = "TEST_VALUE";
		Assert.assertEquals(cache.get(value), value);
	}

	@Test
	public void testDBIndex(){
		ServerAddress serverAddress = new ServerAddress("localhost", redisServerPort, false, 0);
		DataCache<String> cache = CacheBuilder.newBuilder().withCachingMode(CachingMode.STAND_ALONE_REDIS_CACHE)
								.withServerAddress(serverAddress).build();

		String key = "TEST_KEY";
		String value = "TEST_VALUE";
		cache.store(key, value);
		Assert.assertEquals(value, cache.get(key));
	}

	@Ignore
	@Test
	public void testDBIndexExclusivity(){
		ServerAddress serverAddress_0 = new ServerAddress("localhost", redisServerPort, false, 0);
		ServerAddress serverAddress_1 = new ServerAddress("localhost", redisServerPort, false, 1);

		DataCache<String> cache_0 = CacheBuilder.newBuilder().withCachingMode(CachingMode.STAND_ALONE_REDIS_CACHE)
				.withServerAddress(serverAddress_0).withCacheId("my-cache").build();

		DataCache<String> cache_1 = CacheBuilder.newBuilder().withCachingMode(CachingMode.STAND_ALONE_REDIS_CACHE)
				.withServerAddress(serverAddress_1).withCacheId("my-cache").build();

		String key = "TEST_KEY";
		String value = "TEST_VALUE";
		cache_0.store(key, value);
		Assert.assertNotEquals(value, cache_1.get(key));
		Assert.assertNull(cache_1.get(key));
	}

	@Ignore
	@Test
	public void testDBIndexExclusivity2(){
		ServerAddress serverAddress_0 = new ServerAddress("localhost", redisServerPort, false, 0);
		ServerAddress serverAddress_1 = new ServerAddress("localhost", redisServerPort, false, 1);

		DataCache<String> cache_0 = CacheBuilder.newBuilder().withCachingMode(CachingMode.STAND_ALONE_REDIS_CACHE)
				.withServerAddress(serverAddress_0).withCacheId("my-cache").build();

		DataCache<String> cache_1 = CacheBuilder.newBuilder().withCachingMode(CachingMode.STAND_ALONE_REDIS_CACHE)
				.withServerAddress(serverAddress_1).withCacheId("my-cache").build();

		String key = "TEST_KEY";
		String value1 = "TEST_VALUE_1";
		String value2 = "TEST_VALUE_2";
		cache_0.store(key, value1);
		cache_1.store(key, value2);

		Assert.assertEquals(value1, cache_0.get(key));
		Assert.assertNotEquals(value2, cache_0.get(key));

		Assert.assertEquals(value2, cache_1.get(key));
		Assert.assertNotEquals(value1, cache_1.get(key));
	}
}
