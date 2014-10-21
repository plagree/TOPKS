package org.dbweb.Arcomem.datastructures;

import java.util.HashSet;

public class Keywords {
	private HashSet<String>  query;

	public Keywords(HashSet<String> query) {
		super();
		this.query = query;
	}

	public HashSet<String> getQuery() {
		return query;
	}

	public void setQuery(HashSet<String> query) {
		this.query = query;
	}

	public Keywords() {
		super();
		query=new HashSet<String>();
	}
	
	public void addtotQuery(String kwd) {
		this.query.add(kwd);
	}
	
	public void clearQuery(){
		this.query.clear();
	}
	

}
