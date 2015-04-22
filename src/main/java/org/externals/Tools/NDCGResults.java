package org.externals.Tools;

import java.util.ArrayList;
import java.util.List;

public class NDCGResults {

	private List<Long> times;
	private List<Double> ndcgs;
	
	public NDCGResults() {
		times = new ArrayList<Long>();
		ndcgs = new ArrayList<Double>();
	}
	
	public void addPoint(long time, double ndcg) {
		times.add(time);
		ndcgs.add(ndcg);
	}
	
	public List<Long> getTimes() {
		return times;
	}
	public void setTimes(List<Long> times) {
		this.times = times;
	}
	public List<Double> getNdcgs() {
		return ndcgs;
	}
	public void setNdcgs(List<Double> ndcgs) {
		this.ndcgs = ndcgs;
	}
	
	public int size() {
		return times.size();
	}
	
}
