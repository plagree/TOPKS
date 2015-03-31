package org.dbweb.Arcomem.datastructures;
/**
 * keep track of topks execution environment
 * @author aminebaazizi
 *
 */
public class TopksMetadata {
	int numloops;
	int exectime;
	int visitedDocs;
	
	public void print() {
		System.out.println("numloops"+numloops+"|");
//		System.out.print("exectime"+exectime+"|");
//		System.out.println("visitedDocs"+visitedDocs);

	}
	public int getNumloops() {
		return numloops;
	}
	public void setNumloops(int numloops) {
		this.numloops = numloops;
	}
	public int getExectime() {
		return exectime;
	}
	public void setExectime(int exectime) {
		this.exectime = exectime;
	}
	public int getVisitedDocs() {
		return visitedDocs;
	}
	public void setVisitedDocs(int visitedDocs) {
		this.visitedDocs = visitedDocs;
	}
	
}
