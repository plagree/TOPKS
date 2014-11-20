package org.dbweb.socialsearch.topktrust.datastructure;

import java.util.Comparator;

public class ItemBestScoreComparator implements Comparator<Item<String>> {

	public int compare(Item<String> a, Item<String> b)
	{
		if (a.getBestscore() > b.getBestscore())
            return -1;
        else if(a.getBestscore() < b.getBestscore())
            return 1;
        else 
        	return a.getItemId().compareTo(b.getItemId());
	}
	
}