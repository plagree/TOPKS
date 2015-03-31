/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbweb.socialsearch.topktrust.datastructure.general;

/**
 *
 * @author Silver
 */
public class Edge<V,E> {
    
    private V first_vertex;
    private V second_vertex;
    private boolean directed;
    private E label;
    private boolean visited;
    
    /**
     * Edge associates vtx1 and vtx2; labeled with label
     * directed if "directed" set true
     */
    public Edge(V vtx1, V vtx2, E label,boolean directed)
    {
        this.first_vertex = vtx1;
        this.second_vertex = vtx2;
        this.label = label;
        this.directed = directed;
        this.visited = false;

    }

    /**     
     * @return the first vertex in the edge
     */
    public V here(){
        return this.first_vertex;
    }

    /**
     * 
     * @return the second vertex in the edge
     */
    public V there(){
        return this.second_vertex;
    }

    /**
     * Sets the new label of the edge
     * @param label the new label of the edge
     */
    public void setLabel(E label){
        this.label = label;
    }

    /**
     * 
     * @return the label of the edge
     */
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
    
    public boolean isDirected(){
        return this.directed;
    }
    
    public void reset(){
        this.visited = false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.first_vertex != null ? this.first_vertex.hashCode() : 0);
        hash = 37 * hash + (this.second_vertex != null ? this.second_vertex.hashCode() : 0);
        hash = 37 * hash + (this.label != null ? this.label.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object o){
        if(o!=null){
            if(o instanceof Edge)
                if((((Edge)o).here()==this.first_vertex)&&
                   (((Edge)o).there()==this.second_vertex)&&
                   (((Edge)o).isDirected()==this.directed))
                    return true;
        }
        return false;
    }
}
