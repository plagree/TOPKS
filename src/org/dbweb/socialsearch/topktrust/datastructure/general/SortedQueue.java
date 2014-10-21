package org.dbweb.socialsearch.topktrust.datastructure.general;

import java.util.ArrayList;
import java.util.Iterator;

public class SortedQueue<E extends Comparable<E>> {
	
	private ArrayList<E> elements;
	private int size;
	private int maxsize;
	
	private int total_interchanges;

	public SortedQueue(){
		this.elements = new ArrayList<E>();
		elements.add(null);
		size = 0;
		total_interchanges = 0;
		maxsize = 0;
	}
	
	public SortedQueue(SortedQueue<E> queue){
		this.elements = new ArrayList<E>(queue.elements);
		this.size = queue.size;
		this.total_interchanges = queue.total_interchanges;
		this.maxsize = queue.maxsize;
	}
	
	public E getMax() {		
		return elements.get(1);
	}

	public E removeMax() {
		if(size<1) return null;
		E max = elements.get(1);
		elements.set(1, elements.get(size));
		elements.remove(size);
		size--;
		if(size>0) max_heapify(1);
		return max;
	}

	public void add(E value) {
		size++;
		elements.ensureCapacity(size+1);
		elements.add(value);
		if(this.maxsize<size) maxsize = size;
		int i = size;
		while(i>1 && elements.get(parent(i)).compareTo(elements.get(i))<0){
			E element_i = elements.get(i);
			E element_par = elements.get(parent(i));
			elements.set(i, element_par);
			elements.set(parent(i), element_i);
			total_interchanges++;
			i=parent(i);
		}
	}
	
	public void modify_element(E new_value){		
		int index = elements.indexOf(new_value);
		if(index>=1&&index<=size){
			elements.set(index, new_value);
			int i=size;
			while(i>1 && elements.get(parent(i)).compareTo(elements.get(i))<0){
				E element_i = elements.get(i);
				E element_par = elements.get(parent(i));
				elements.set(i, element_par);
				elements.set(parent(i), element_i);
				total_interchanges++;
				i=parent(i);
			}
		}
	}

	public int size() {
		return size;
	}		
	
	public void rebuild_heap(){
		for(int i=(int)Math.floor(size/2);i>=1;i--)
			max_heapify(i);
	}
	
	private void max_heapify(int i){
		int l = left(i);
		int r = right(i);
		int largest;
		if(l<=size && elements.get(l).compareTo(elements.get(i))>0)
			largest = l;		
		else
			largest = i;
		if(r<=size && elements.get(r).compareTo(elements.get(largest))>0)
			largest = r;
		if(largest!=i){
			E element_i = elements.get(i);
			E element_larg = elements.get(largest);
			elements.set(i, element_larg);
			elements.set(largest, element_i);
			total_interchanges++;
			max_heapify(largest);
		}
	}
	
	public Iterator<E> getElementsIterator(){
		return elements.iterator();
	}
	
	private int parent(int i){
		return (int)Math.floor(i/2);
	}
	
	private int left(int i){
		return i*2;
	}
	
	private int right(int i){
		return i*2+1;
	}
	
	public int getInterchanges(){
		return total_interchanges;
	}
	
	public int getMaxsize(){
		return maxsize;
	}
	

}
