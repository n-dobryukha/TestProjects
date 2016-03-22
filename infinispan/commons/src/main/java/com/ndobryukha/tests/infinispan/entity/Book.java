package com.ndobryukha.tests.infinispan.entity;

/**
 * Created by Nikita_Dobriukha on 2016-03-17.
 */
public class Book {
	String title;
	String description;
	int publishYear;

	public Book() {
	}

	public Book(String title, String description, int publishYear) {
		this.title = title;
		this.description = description;
		this.publishYear = publishYear;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getPublishYear() {
		return publishYear;
	}

	public void setPublishYear(int publishYear) {
		this.publishYear = publishYear;
	}

	@Override
	public String toString() {
		return "Book{" +
				"title='" + title + '\'' +
				", description='" + description + '\'' +
				", publishYear=" + publishYear +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Book book = (Book) o;

		if (publishYear != book.publishYear) return false;
		if (!title.equals(book.title)) return false;
		return description.equals(book.description);

	}

	@Override
	public int hashCode() {
		int result = title.hashCode();
		result = 31 * result + description.hashCode();
		result = 31 * result + publishYear;
		return result;
	}
}
