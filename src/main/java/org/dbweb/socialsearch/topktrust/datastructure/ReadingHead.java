package org.dbweb.socialsearch.topktrust.datastructure;

public class ReadingHead implements Comparable<ReadingHead> {
	/**
	 * Reading head on an inverted list
	 */

	private String completion;
	private long item;
	private int value;
	
	/**
	 * 
	 * @param completion : inverted list term
	 * @param item : item of the reading head
	 * @param value : value of the reading head
	 */
	public ReadingHead(String completion, long item, int value) {
		this.completion = completion;
		this.item = item;
		this.value = value;
	}

	public String getCompletion() {
		return completion;
	}

	public void setCompletion(String completion) {
		this.completion = completion;
	}

	public long getItem() {
		return item;
	}

	public void setItem(long item) {
		this.item = item;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}
	
	@Override
	public int compareTo(ReadingHead obj) {
		return -(this.value - obj.value);
	}
	
    @Override
    public int hashCode() {
        return (this.value+this.item+this.completion).hashCode();
    }
    
    @Override
    public String toString() {
    	return "Reading head of term " + this.completion + " with value " + this.value + " and item " + this.item; 
    }
}
