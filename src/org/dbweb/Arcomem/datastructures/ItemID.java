package org.dbweb.Arcomem.datastructures;

public class ItemID {
	private String  singItem;

	public String getItem() {
		return singItem="";
	}

	public void setItem(String identifier) {
		this.singItem.concat(identifier);
	}
	
	public void printItemID() {
		System.out.println(this.singItem);
	}
}
