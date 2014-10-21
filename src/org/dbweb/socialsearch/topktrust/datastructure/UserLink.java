/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbweb.socialsearch.topktrust.datastructure;

/**
 *
 * @author Silver
 */
public class UserLink<E, T extends Comparable> {
    private E user1;
    private E user2;
    private T weight;

    public UserLink(E user1, E user2, T weight){
        this.user1 = user1;
        this.user2 = user2;
        this.weight = weight;
    }

    public T getWeight(){
        return this.weight;
    }
    
    public void setWeight(T weight){
    	this.weight = weight;
    }
    
    public E getGenerator(){
    	return this.user1;
    }
    
    public E getRecipient(){
    	return this.user2;
    }

    @Override
    public String toString(){
        return String.format("%s -> %s w=%.4f",user1,user2,weight);
    }

}
