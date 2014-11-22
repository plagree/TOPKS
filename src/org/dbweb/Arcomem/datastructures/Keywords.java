package org.dbweb.Arcomem.datastructures;

import java.util.ArrayList;

public class Keywords {
	private ArrayList<String>  query;

	public Keywords(ArrayList<String> query) {
		super();
		this.query = query;
	}

	public ArrayList<String> getQuery() {
		return query;
	}

	public void setQuery(ArrayList<String> query) {
		this.query = query;
	}

	public Keywords() {
		super();
		query=new ArrayList<String>();
	}
	
	public void addtotQuery(String kwd) {
		this.query.add(kwd);
	}
	
	public void clearQuery(){
		this.query.clear();
	}
	

}
