package com.ndobryukha.tests.infinispan;


import com.ndobryukha.tests.infinispan.entity.Book;
import com.ndobryukha.tests.infinispan.entity.User;
import com.ndobryukha.tests.infinispan.marshaller.BookMarshaller;
import com.ndobryukha.tests.infinispan.marshaller.UserMarshaller;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.logging.Log;
import org.infinispan.client.hotrod.logging.LogFactory;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.commons.util.Util;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.remote.client.MarshallerRegistration;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Created by Nikita_Dobriukha on 2016-03-18.
 */
public class TestRealHotRodServer extends Assert {
	private static final Log LOG = LogFactory.getLog(TestRealHotRodServer.class);

	private static final String DEFAULT_CACHE_NAME = "default";
	private static final String BOOKS_CACHE_NAME = "booksCache";
	private static final String USERS_CACHE_NAME = "usersCache";


	private static final RemoteCacheManager remoteCacheManager = createRemoteCacheManager();

	@BeforeClass
	public static void setUp() throws Exception {
		configureRemoteCacheManager();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		remoteCacheManager.stop();
	}

	private static RemoteCacheManager createRemoteCacheManager() {
		ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.addServer()
				.host("10.11.13.220")
				.port(11222)
				.marshaller(new ProtoStreamMarshaller());
		return new RemoteCacheManager(builder.build());
	}

	private static void configureRemoteCacheManager() throws IOException {
		RemoteCache<String, String> metadataCache = remoteCacheManager.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
		metadataCache.put("book.proto", Util.read(TestRealHotRodServer.class.getResourceAsStream("/book.proto")));
		metadataCache.put("user.proto", Util.read(TestRealHotRodServer.class.getResourceAsStream("/user.proto")));
		assertFalse(metadataCache.containsKey(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX));
		SerializationContext srzCtx = ProtoStreamMarshaller.getSerializationContext(remoteCacheManager);
		FileDescriptorSource fds = new FileDescriptorSource();
		fds.addProtoFiles("/book.proto");
		fds.addProtoFiles("/user.proto");
		srzCtx.registerProtoFiles(fds);
		srzCtx.registerMarshaller(new BookMarshaller());
		srzCtx.registerMarshaller(new UserMarshaller());
		MarshallerRegistration.registerMarshallers(srzCtx);
	}

	@Test
	public void testDefaultCache() {
		int count = 50;
		RemoteCache<String, String> cache = remoteCacheManager.getCache(DEFAULT_CACHE_NAME);
		for (int i=0; i<count; i++) {
			cache.put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
		}
		assertEquals(count, cache.size());
		cache.clear();
	}

	@Test
	public void testBooksCache() throws IOException {
		RemoteCache<Long, Book> cache = remoteCacheManager.getCache(BOOKS_CACHE_NAME);
		cache.put(UUID.randomUUID().getMostSignificantBits(), new Book("title", "description", 2000));
		QueryFactory qf = Search.getQueryFactory(cache);
		Query query = qf.from(Book.class)
				.having("title").eq("title").toBuilder()
				.build();
		List<Book> list = query.list();
		assertNotNull(list);
		cache.clear();
		assertFalse(list.isEmpty());
	}


	@Test
	public void testBooksCacheBenchmark() {
		final int MAX_CACHE_COUNT = 100_000;
		RemoteCache<Long, Book> cache = remoteCacheManager.getCache(BOOKS_CACHE_NAME);
		for (int i = 0; i < MAX_CACHE_COUNT; i++) {
			cache.put(UUID.randomUUID().getMostSignificantBits(), new Book("title" + i, "description" + i, 2000));
		}
		QueryFactory qf = Search.getQueryFactory(cache);
		for (int i = 0; i < 250; i++) {
			Query query = qf.from(Book.class).having("title").eq("title" + i*100).toBuilder().build();
			long startSearch = System.currentTimeMillis();
			List<Book> list = query.list();
			long finish = System.currentTimeMillis();
			LOG.info(String.format("search #%d: time = %.3fs", i, (finish - startSearch)/1000.0));
		}
		cache.clear();
	}

	@Test
	public void testUsersCache() throws IOException {
		RemoteCache<Long, User> cache = remoteCacheManager.getCache(USERS_CACHE_NAME);
		cache.put(UUID.randomUUID().getMostSignificantBits(), new User(UUID.randomUUID().getMostSignificantBits(), "userName"));
		QueryFactory qf = Search.getQueryFactory(cache);
		Query query = qf.from(User.class)
				.having("userName").eq("userName").toBuilder()
				.build();
		List<User> list = query.list();
		assertNotNull(list);
		cache.clear();
		assertFalse(list.isEmpty());
	}

}
