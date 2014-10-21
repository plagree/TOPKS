package org.dbweb.socialsearch.topktrust.datastructure.general;

public interface IPriorityQueue<E extends Comparable<E>>
{
    public E getFirst();

    public E remove();

    public void add(E value);

    public boolean isEmpty();

    public int size();

    public void clear();
    
    public void refresh();
}
