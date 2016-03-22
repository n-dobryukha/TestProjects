package com.ndobryukha.tests.infinispan;

import com.ndobryukha.tests.infinispan.entity.Book;
import com.ndobryukha.tests.infinispan.marshaller.BookMarshaller;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.commons.equivalence.ByteArrayEquivalence;
import org.infinispan.commons.util.Util;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Index;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.jmx.PlatformMBeanServerLookup;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.remote.client.MarshallerRegistration;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.junit.*;

import java.io.IOException;
import java.util.List;
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
	public static void setUp() throws IOException {
		startServer();
		configureRemoteCacheManager();
	}

	@AfterClass
	public static void tearDown() {
		server.stop();
		remoteCacheManager.stop();
	}


	@Ignore
	@Test
	public void testRemoteCacheManager() {
		RemoteCache<String, Book> remoteCache = remoteCacheManager.getCache(TEST_CACHE_NAME);
		assertNotNull(remoteCache);
		String key = UUID.randomUUID().toString();
		Book entity = new Book("title", "description", 2000);
		remoteCache.put(key, entity);
		assertEquals(remoteCache.size(), 1);
		assertEquals(entity, remoteCache.get(key));
		QueryFactory qf = Search.getQueryFactory(remoteCache);
		Query query = qf.from(Book.class)
				.having("title").eq("title").toBuilder()
				.build();
		List<Book> books = query.list();
		assertFalse(books.isEmpty());
		assertEquals(entity, books.get(0));
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

	private static void configureRemoteCacheManager() throws IOException {
		remoteCacheManager.getCache(TEST_CACHE_NAME);
		//initialize server-side serialization
		RemoteCache<String, String> metadataCache = remoteCacheManager.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
		metadataCache.put("book.proto", Util.read(TestSimpleProgramHotRodServer.class.getResourceAsStream("/book.proto")));
		assertFalse(metadataCache.containsKey(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX));
		SerializationContext srzCtx = ProtoStreamMarshaller.getSerializationContext(remoteCacheManager);
		FileDescriptorSource fds = new FileDescriptorSource();
		fds.addProtoFiles("/book.proto");
		srzCtx.registerProtoFiles(fds);
		srzCtx.registerMarshaller(new BookMarshaller());
		MarshallerRegistration.registerMarshallers(srzCtx);
	}
}
