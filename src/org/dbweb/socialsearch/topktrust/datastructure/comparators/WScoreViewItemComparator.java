/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbweb.socialsearch.topktrust.datastructure.comparators;

import org.dbweb.socialsearch.topktrust.datastructure.Item;
import org.dbweb.socialsearch.topktrust.datastructure.views.ViewItem;

import java.util.Comparator;

/**
 *
 * @author Silver
 */
public class WScoreViewItemComparator implements Comparator<ViewItem> {

    public int compare(ViewItem o1, ViewItem o2) {
    	if(o1.getWscore()<o2.getWscore())
    		return 1;
    	else if(o1.getWscore()>o2.getWscore())
    		return -1;
    	else if(o1.getBscore()>o2.getBscore())
    		return -1;
    	else
    		return o1.getId().compareTo(o2.getId());
    }

}
