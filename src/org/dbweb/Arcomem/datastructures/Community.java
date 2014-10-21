package org.dbweb.Arcomem.datastructures;

import java.util.ArrayList;

public class Community {
	
	private ArrayList<Seeker> users;
	private String id;
	
	public String getId() {
		return id;
	}



	public void setId(String id) {
		this.id = id;
	}



	public Community()
	{
		this.users=new ArrayList<Seeker>();
	}
			
	
	
	public Community(int[] users, String name){
		this.users= new ArrayList<Seeker>();
		for(int id:users){
			Seeker seeker = new Seeker();
			seeker.setUserId(id);
			this.users.add(seeker);
		}
		this.id=name;
	}
	public Community(ArrayList<Seeker> users, String name) {
		super();
		this.users = users;
		this.id = name;
	}

	public ArrayList<Seeker> getUsers() {
		return users;
	}
	
	public void setUsers(ArrayList<Seeker> users) {
		this.users = users;
	}
	
	public int getSize()
	{
		return users.size();
	}

}
