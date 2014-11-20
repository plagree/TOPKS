package org.dbweb.socialsearch.topktrust.algorithm;

public class DocumentNumTag implements Comparable<DocumentNumTag>{
	private String docId;
	private int num;
	
	public DocumentNumTag(String docId, int num) {
		this.docId = docId;
		this.num = num;
	}
	
	public int getNum() {
		return this.num;
	}
	
	public String getDocId() {
		return this.docId;
	}
	
	@Override
	public int compareTo(DocumentNumTag o) {
		// TODO Auto-generated method stub
		return this.num - o.getNum();
	}
	
}