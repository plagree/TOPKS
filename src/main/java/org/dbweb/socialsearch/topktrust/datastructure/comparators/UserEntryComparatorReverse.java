/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbweb.socialsearch.topktrust.datastructure.comparators;

import org.dbweb.socialsearch.topktrust.datastructure.UserEntry;

import java.util.Comparator;

/**
 *
 * @author Silver
 */
public class UserEntryComparatorReverse implements Comparator<UserEntry> {

    public int compare(UserEntry o1, UserEntry o2) {
        return -(o1.getDist().compareTo(o2.getDist()));
    }


}
