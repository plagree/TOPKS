package org.dbweb.socialsearch.shared;

import java.io.Serializable;
import java.util.ArrayList;

public class ResultsEncoding implements Serializable {
	
	private String[] results;
	private String statistics;
	private ArrayList<Double> weights;
	private ArrayList<Integer> visited;
	
	private String statistics2;
	private ArrayList<Double> weights2;
	
	private boolean compare = false;
	
	public ResultsEncoding(){
	}

	public void setResults(String[] results) {
		this.results = results;
	}

	public String[] getResults() {
		return results;
	}

	public String getStatistics() {
		return statistics;
	}

	public void setStatistics(String statistics) {
		this.statistics = statistics;
	}

	public ArrayList<Double> getWeights() {
		return weights;
	}

	public void setWeights(ArrayList<Double> weights) {
		this.weights = weights;
	}

	public String getStatistics2() {
		return statistics2;
	}

	public void setStatistics2(String statistics2) {
		this.statistics2 = statistics2;
	}

	public ArrayList<Double> getWeights2() {
		return weights2;
	}

	public void setWeights2(ArrayList<Double> weights2) {
		this.weights2 = weights2;
	}

	public boolean isCompare() {
		return compare;
	}

	public void setCompare(boolean compare) {
		this.compare = compare;
	}

	public ArrayList<Integer> getVisited() {
		return visited;
	}

	public void setVisited(ArrayList<Integer> visited) {
		this.visited = visited;
	}
	
	

}
