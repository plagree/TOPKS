/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbweb.socialsearch.topktrust.datastructure.comparators;

import org.dbweb.socialsearch.topktrust.datastructure.Item;

import java.util.Comparator;

/**
 *
 * @author Silver
 */
public class MinScoreItemComparator implements Comparator<Item> {

    public int compare(Item o1, Item o2) {
        if(o1.getComputedScore()<o2.getComputedScore())
            return -1;
        else if(o1.getComputedScore()>o2.getComputedScore())
            return 1;
        else if(o1.getComputedScore()==o2.getComputedScore()){
        	if(o1.getBestscore()<o2.getBestscore())
        		return -1;
        	else if(o1.getBestscore()>o2.getBestscore())
        		return 1;
        	else
        		return (o1.getItemId()<o2.getItemId()) ? -1 : 1;
        }
        else
        	return 1;
    }

}
