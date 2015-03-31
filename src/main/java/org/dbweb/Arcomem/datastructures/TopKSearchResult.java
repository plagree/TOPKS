package org.dbweb.Arcomem.datastructures;

import java.util.ArrayList;

public class TopKSearchResult extends BasicSearchResult{

	int k;
	
	public int getK() {
		return k;
	}

	public void setK(int k) {
		this.k = k;
	}

	public TopKSearchResult() {
		// TODO Auto-generated constructor stub
		super();
	}

	public TopKSearchResult(ArrayList<String> result) {
		super(result);
		// TODO Auto-generated constructor stub
	}
	
	public TopKSearchResult(ArrayList<String> result, int k) {
		super(result);
		this.k=k;
	}

}
