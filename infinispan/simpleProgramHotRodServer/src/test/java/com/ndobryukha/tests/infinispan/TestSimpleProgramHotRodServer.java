package com.ndobryukha.tests.infinispan;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.commons.equivalence.ByteArrayEquivalence;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Index;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.jmx.PlatformMBeanServerLookup;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.junit.*;

import java.util.UUID;

/**
 * Created by Nikita_Dobriukha on 2016-03-17.
 */
public class TestSimpleProgramHotRodServer extends Assert {
	private static final String TEST_SERVER_HOST = "127.0.0.1";
	private static final int TEST_SERVER_PORT = 11333;
	private static final String TEST_CACHE_NAME = "testCache";

	private static final HotRodServer server = new HotRodServer();
	private static final RemoteCacheManager remoteCacheManager = createRemoteCacheManager();

	@BeforeClass
	public static void setUp() {
		startServer();
	}

	@AfterClass
	public static void tearDown() {
		server.stop();
		remoteCacheManager.stop();
	}


	@Test
	public void testRemoteCacheManager() {
		RemoteCache<String, String> remoteCache = remoteCacheManager.getCache(TEST_CACHE_NAME);
		assertNotNull(remoteCache);
	}

	private static void startServer() {
		HotRodServerConfigurationBuilder serverConfigurationBuilder = new HotRodServerConfigurationBuilder()
				.host(TEST_SERVER_HOST).port(TEST_SERVER_PORT);
		server.start(serverConfigurationBuilder.build(), createEmbeddedCacheManager());
	}


	private static EmbeddedCacheManager createEmbeddedCacheManager() {
		Configuration cacheConfiguration = createCacheConfigurationBuilder().build();

		GlobalConfigurationBuilder globalConfigurationBuilder = new GlobalConfigurationBuilder().nonClusteredDefault();
		globalConfigurationBuilder.globalJmxStatistics()
				.enable()
				.allowDuplicateDomains(false)
				.jmxDomain("TestProgramHotRodServer-" + UUID.randomUUID().toString())
				.mBeanServerLookup(new PlatformMBeanServerLookup());
		GlobalConfiguration globalConfiguration = globalConfigurationBuilder.build();
		EmbeddedCacheManager cacheManager = new DefaultCacheManager(globalConfiguration, new ConfigurationBuilder().build(globalConfiguration), true);

		cacheManager.defineConfiguration(TEST_CACHE_NAME, cacheConfiguration);
		return cacheManager;
	}

	private static ConfigurationBuilder createCacheConfigurationBuilder() {
		ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.dataContainer()
				.keyEquivalence(ByteArrayEquivalence.INSTANCE)
				.indexing().index(Index.ALL)
				.addProperty("default.directory_provider", "ram")
				.addProperty("lucene_version", "LUCENE_CURRENT");
		return builder;
	}

	private static RemoteCacheManager createRemoteCacheManager() {
		org.infinispan.client.hotrod.configuration.ConfigurationBuilder clientBuilder = new org.infinispan.client.hotrod.configuration.ConfigurationBuilder();
		clientBuilder.addServer().host(TEST_SERVER_HOST).port(TEST_SERVER_PORT);
		clientBuilder.marshaller(new ProtoStreamMarshaller());
		return new RemoteCacheManager(clientBuilder.build());
	}

}
