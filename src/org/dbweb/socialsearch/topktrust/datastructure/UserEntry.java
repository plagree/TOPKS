/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbweb.socialsearch.topktrust.datastructure;

import java.util.Comparator;

/**
 *
 * @author Silver
 */
public class UserEntry<T extends Comparable<T>> implements Comparable<UserEntry<T>>{

    private int entryId;
    private T d;
    private UserEntry<T> pred;

    public UserEntry(int entryId, T d){
        this.entryId = entryId;
        this.d = d;
        this.pred = null;
    }
    
    public UserEntry(UserEntry<T> oldUser){
    	this.entryId = oldUser.entryId;
    	this.d = oldUser.d;
    	this.pred = oldUser.pred;
    }

    public int getEntryId(){
        return this.entryId;
    }

    public T getDist(){
        return this.d;
    }

    public void setDist(T d){
        this.d = d;
    }

    public UserEntry<T> getPred(){
        return this.pred;
    }

    public void setPred(UserEntry<T> pred){
        this.pred = pred;
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + entryId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserEntry other = (UserEntry) obj;
		if (entryId != other.entryId)
			return false;
		return true;
	}

    @Override
    public String toString(){
        int predUser;
        if(pred==null) predUser = 0;
        else predUser=pred.getEntryId();
        return String.format("{ID:%d d=%.5f pred=%s}",entryId,d,predUser);
    }

    public int compareTo(UserEntry<T> o) {
        return -d.compareTo(o.getDist());
    }

}
