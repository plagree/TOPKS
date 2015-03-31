package org.dbweb.Arcomem.datastructures;

public class Seeker {
	String seekerID;
	String political;
	String category;
	public Seeker(String userId) {
		super();
		this.seekerID = userId;		
	}
	public void clear() {
		this.seekerID=null;
	}
	
	public Seeker() {
		super();		
	}
	
	

	public String getUserId() {
		return seekerID;
	}

	public void setUserId(String userId) {
		this.seekerID = userId;
	}

	public void setUserId(int userId) {
		this.seekerID = String.valueOf(userId);
	}
	
	public String getPolitical() {
		return political;
	}

	public void setPolitical(String political) {
		this.political = political;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}
	
}
