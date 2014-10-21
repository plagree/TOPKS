/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbweb.socialsearch.topktrust.datastructure.general;

import java.util.Collection;
import java.util.Iterator;
import java.util.Enumeration;

/**
 *
 * @author Silver
 */
public interface IStructure<E> {
    public int size();
    public boolean isEmpty();
    public void clear();
    public boolean contains(Object value);
    public void add(Object value);
    public Object remove(Object value);
    public Enumeration elements();
    public Iterator iterator();
    public Collection value();
}
