package com.ndobryukha.tests.infinispan.marshaller;

import com.ndobryukha.tests.infinispan.entity.User;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

/**
 * Created by Nikita_Dobriukha on 2016-03-21.
 */
public class UserMarshaller implements MessageMarshaller<User> {
	@Override
	public User readFrom(ProtoStreamReader reader) throws IOException {
		Long id = reader.readLong("id");
		String userName = reader.readString("userName");
		return new User(id, userName);
	}

	@Override
	public void writeTo(ProtoStreamWriter writer, User user) throws IOException {
		writer.writeLong("id", user.getId());
		writer.writeString("userName", user.getUserName());
	}

	@Override
	public Class<? extends User> getJavaClass() {
		return User.class;
	}

	@Override
	public String getTypeName() {
		return "com.ndobryukha.tests.infinispan.entity.User";
	}
}
