/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbweb.socialsearch.topktrust.datastructure.general;

/**
 *
 * @author Silver
 */
public class Vertex<E> {

    private E label;
    private boolean visited;

    public Vertex(E label){
        this.label = label;
        this.visited = false;
    }

    public E label(){
        return this.label;
    }

    public boolean visit(){
        boolean prev_visited = this.visited;
        this.visited = true;
        return prev_visited;
    }

    public boolean isVisited(){
        return this.visited;
    }

    public void reset(){
        this.visited = false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 31 * hash + (this.label != null ? this.label.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object o){
        if(o!=null){
            if(o instanceof Vertex)
                if(((Vertex)o).label()==this.label)
                    return true;
        }
        return false;
    }
}
