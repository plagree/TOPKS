package org.dbweb.socialsearch.topktrust.datastructure;

import java.util.Comparator;

public class ItemAverageScoreComparator implements Comparator<Item<String>> {

	public int compare(Item<String> a, Item<String> b)
	{
		if((a.getBestscore()+a.getComputedScore()) >= (b.getBestscore()+b.getComputedScore()))
            return -1;
        else if((a.getBestscore()+a.getComputedScore()) < (b.getBestscore()+b.getComputedScore()))
            return 1;
        else 
        	return a.getItemId().compareTo(b.getItemId());
	}
	
}
