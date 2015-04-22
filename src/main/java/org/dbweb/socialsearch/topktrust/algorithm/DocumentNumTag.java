package org.dbweb.socialsearch.topktrust.algorithm;

public class DocumentNumTag implements Comparable<DocumentNumTag>{
	private long docId;
	private int num;
	
	public DocumentNumTag(long docId, int num) {
		this.docId = docId;
		this.num = num;
	}
	
	public int getNum() {
		return this.num;
	}
	
	public long getDocId() {
		return this.docId;
	}
	
	@Override
	public int compareTo(DocumentNumTag o) {
		return this.num - o.getNum();
	}
	
	@Override
	public String toString() {
		return "Item: "+this.docId+", value: "+this.num;
	}
	
}