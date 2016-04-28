/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ndobryukha.tests.infinispan.benchmark;

import com.ndobryukha.tests.infinispan.entity.Book;
import com.ndobryukha.tests.infinispan.marshaller.BookMarshaller;
import com.ndobryukha.tests.infinispan.marshaller.UserMarshaller;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.commons.util.Util;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.remote.client.MarshallerRegistration;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@State(Scope.Benchmark)
@Measurement(iterations = 100)
@Fork(value = 1)
@BenchmarkMode(Mode.SingleShotTime)
public class MyBenchmark {
    private static final String BOOKS_CACHE_NAME = "booksCache";

    private static final RemoteCacheManager remoteCacheManager = createRemoteCacheManager();

    private static final Random random = new Random();

    private static final int MAX_CACHE_COUNT = 500_000;

    private static RemoteCacheManager createRemoteCacheManager() {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.addServer()
                .host("10.11.13.220")
                .port(11222)
                .marshaller(new ProtoStreamMarshaller());
        return new RemoteCacheManager(builder.build());
    }

    @Setup
    public static void configureRemoteCacheManager() throws IOException {
        RemoteCache<String, String> metadataCache = remoteCacheManager.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
        metadataCache.put("book.proto", Util.read(MyBenchmark.class.getResourceAsStream("/book.proto")));
        metadataCache.put("user.proto", Util.read(MyBenchmark.class.getResourceAsStream("/user.proto")));
        SerializationContext srzCtx = ProtoStreamMarshaller.getSerializationContext(remoteCacheManager);
        FileDescriptorSource fds = new FileDescriptorSource();
        fds.addProtoFiles("/book.proto");
        fds.addProtoFiles("/user.proto");
        srzCtx.registerProtoFiles(fds);
        srzCtx.registerMarshaller(new BookMarshaller());
        srzCtx.registerMarshaller(new UserMarshaller());
        MarshallerRegistration.registerMarshallers(srzCtx);

        populateCache();
    }

    @TearDown
    public static void cleanCache() {
        remoteCacheManager.getCache(BOOKS_CACHE_NAME).clear();
    }

    private static void populateCache() {
        RemoteCache<Long, Book> cache = remoteCacheManager.getCache(BOOKS_CACHE_NAME);
        for (int i = 0; i < MAX_CACHE_COUNT; i++) {
            cache.put(UUID.randomUUID().getMostSignificantBits(), new Book("title" + i, "description" + i, 2000));
        }
    }

    @Benchmark
    public void testSearch() {
        RemoteCache<Long, Book> cache = remoteCacheManager.getCache(BOOKS_CACHE_NAME);
        QueryFactory qf = Search.getQueryFactory(cache);
        Query query = qf.from(Book.class).having("title").eq("title" + random.nextInt(MAX_CACHE_COUNT)).toBuilder().build();
        List<Book> list = query.list();
    }

}
