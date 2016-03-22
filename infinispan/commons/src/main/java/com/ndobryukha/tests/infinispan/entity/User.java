package com.ndobryukha.tests.infinispan.entity;

/**
 * Created by Nikita_Dobriukha on 2016-03-21.
 */
public class User {
	private long id;
	private String userName;

	public User() {
	}

	public User(long id, String userName) {
		this.id = id;
		this.userName = userName;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}
}
