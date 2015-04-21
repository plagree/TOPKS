package org.dbweb.socialsearch.topktrust.datastructure;

public class ReadingHead {
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
    public int hashCode() {
        /*int hash = 1;
        hash = hash * 13 + (int)this.item;
        hash = hash * 17 + this.value;
        hash = hash * 31 + this.completion.hashCode();*/
        return (this.value+this.item+this.completion).hashCode();
    }
}
