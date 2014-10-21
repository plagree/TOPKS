package org.dbweb.Arcomem.datastructures;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.dbweb.Arcomem.datastructures.BasicSearchResult;
import org.dbweb.Arcomem.datastructures.Seeker;

public class ComplexSearchResult {
	HashMap<Seeker,BasicSearchResult> Results;//the results per seeker
	BasicSearchResult aggLists;//the result of aggregation	

	public HashMap<Seeker,BasicSearchResult> getAllResults(Seeker seek) {
		return Results;
	}
	
	public BasicSearchResult getResults(Seeker seek) {
		return Results.get(seek);
	}

	
	public void setResults(HashMap<Seeker,BasicSearchResult> results) {
		Results = results;
	}
	

	
	public void addToResults(Seeker seeker, BasicSearchResult singleTopk) {
		
		this.Results.put(seeker, singleTopk);
	
	}
	
	/**
	 * obsolete
	 * @return
	 */
	public String toRMatrices(){
		// produce smth like blist<-list(c(1,3,5),c(4,32,4))
		String str="nlist<-list(";
		int c=0;
		for(BasicSearchResult res: Results.values())
		{
			if(c==0){c++; str+=String.format("c(%s)", res.getCommaSepItems());}
			str+=String.format(",c(%s)", res.getCommaSepItems());
		}
		str=String.format("%s%n%s", "aggregateRanks(nlist)");
		return str;
	}

}
