package org.dbweb.socialsearch.topktrust.datastructure;

import java.util.Comparator;

public class ItemBestScoreComparator implements Comparator<Item> {
	
	public int compare(Item a, Item b)
	{
		if (a.getBestscore() > b.getBestscore())
            return -1;
        else if(a.getBestscore() < b.getBestscore())
            return 1;
        else 
        	return Long.compare(a.getItemId(), b.getItemId());
	}
	
}