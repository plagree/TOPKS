package org.dbweb.socialsearch.topktrust.datastructure;

import java.util.Comparator;

public class ItemAverageScoreComparator implements Comparator<Item> {

    public int compare(Item a, Item b)
    {
        if(a.getBestscore() + a.getComputedWorstScore() > b.getBestscore()+b.getComputedWorstScore())
            return -1;
        else if(a.getBestscore()+a.getComputedWorstScore() < b.getBestscore()+b.getComputedWorstScore())
            return 1;
        else 
            return 0;
    }

}