package org.dbweb.socialsearch.shared;

import java.io.Serializable;

public class BookmarkEncoding implements Serializable {
	
	private int user;
	private String item;
	private String tags;
	
	public BookmarkEncoding(){
	}
	
	public int getUser() {
		return user;
	}
	public void setUser(int user) {
		this.user = user;
	}
	public String getItem() {
		return item;
	}
	public void setItem(String item) {
		this.item = item;
	}
	public String getTags() {
		return tags;
	}
	public void setTags(String tags) {
		this.tags = tags;
	}

}
