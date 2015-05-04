package org.externals.Tools;

import java.util.ArrayList;
import java.util.List;

public class NDCGResults {

	private List<Long> steps;
	private List<Double> ndcgs;
	
	public NDCGResults() {
		steps = new ArrayList<Long>();
		ndcgs = new ArrayList<Double>();
	}
	
	public void addPoint(long step, double ndcg) {
		steps.add(step);
		ndcgs.add(ndcg);
	}
	
	public List<Long> getSteps() {
		return steps;
	}
	public void setSteps(List<Long> steps) {
		this.steps = steps;
	}
	public List<Double> getNdcgs() {
		return ndcgs;
	}
	public void setNdcgs(List<Double> ndcgs) {
		this.ndcgs = ndcgs;
	}
	
	public int size() {
		return steps.size();
	}
	
}
