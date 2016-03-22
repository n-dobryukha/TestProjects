package com.ndobryukha.tests.infinispan.marshaller;

import com.ndobryukha.tests.infinispan.entity.Book;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

/**
 * Created by Nikita_Dobriukha on 2016-03-17.
 */
public class BookMarshaller implements MessageMarshaller<Book> {
	@Override
	public Book readFrom(ProtoStreamReader reader) throws IOException {
		String title = reader.readString("title");
		String description = reader.readString("description");
		int publishYear = reader.readInt("publishYear");
		return new Book(title, description, publishYear);
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, Book book) throws IOException {
		writer.writeString("title", book.getTitle());
		writer.writeString("description", book.getDescription());
		writer.writeInt("publishYear", book.getPublishYear());
	}

	@Override
	public Class<? extends Book> getJavaClass() {
		return Book.class;
	}

	@Override
	public String getTypeName() {
		return "com.ndobryukha.tests.infinispan.entity.Book";
	}
}
