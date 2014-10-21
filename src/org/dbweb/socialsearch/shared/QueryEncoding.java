package org.dbweb.socialsearch.shared;

import java.io.Serializable;
import java.util.ArrayList;

public class QueryEncoding implements Serializable {
	
	//Main query
	private String query;
	private int seeker;
	private int k;
	private float alpha;
	private float delta;
	private boolean heap;
	private int k1;
	private boolean uat;
	private int approxMethod;
	private int network;
	private int score;
	private int function;
	private double coeff;
	private boolean cache;
	private ArrayList<Integer> visited;
	
	//Compared query
	private int seeker_2;
	private double coeff_2;
	private float alpha_2;
	private int function_2;
	private boolean compare;
	
	public QueryEncoding(){
	}
	
	public void setQuery(String query) {
		this.query = query;
	}
	public String getQuery() {
		return query;
	}
	public void setSeeker(int seeker) {
		this.seeker = seeker;
	}
	public int getSeeker() {
		return seeker;
	}
	public void setK(int k) {
		this.k = k;
	}
	public int getK() {
		return k;
	}
	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}
	public float getAlpha() {
		return alpha;
	}
	public void setHeap(boolean heap) {
		this.heap = heap;
	}
	public boolean getHeap() {
		return heap;
	}
	public void setK1(int k1) {
		this.k1 = k1;
	}
	public int getK1() {
		return k1;
	}
	public void setUat(boolean uat) {
		this.uat = uat;
	}
	public boolean isUat() {
		return uat;
	}
	public void setApproxMethod(int approxMethod) {
		this.approxMethod = approxMethod;
	}
	public int getApproxMethod() {
		return approxMethod;
	}
	public void setNetwork(int network) {
		this.network = network;
	}
	public int getNetwork() {
		return network;
	}
	public void setScore(int score) {
		this.score = score;
	}
	public int getScore() {
		return score;
	}
	public void setFunction(int function) {
		this.function = function;
	}
	public int getFunction() {
		return function;
	}
	public void setDelta(float delta) {
		this.delta = delta;
	}
	public float getDelta() {
		return delta;
	}
	public void setCoeff(double coeff) {
		this.coeff = coeff;
	}
	public double getCoeff() {
		return coeff;
	}
	public boolean isCache() {
		return cache;
	}
	public void setCache(boolean cache) {
		this.cache = cache;
	}
	public float getAlpha_2() {
		return alpha_2;
	}
	public void setAlpha_2(float alpha_2) {
		this.alpha_2 = alpha_2;
	}
	
	public int getFunction_2() {
		return function_2;
	}
	public void setFunction_2(int function_2) {
		this.function_2 = function_2;
	}
	public boolean isCompare() {
		return compare;
	}
	public void setCompare(boolean compare) {
		this.compare = compare;
	}
	public int getSeeker_2() {
		return seeker_2;
	}
	public void setSeeker_2(int seeker_2) {
		this.seeker_2 = seeker_2;
	}
	public double getCoeff_2() {
		return coeff_2;
	}
	public void setCoeff_2(double coeff_2) {
		this.coeff_2 = coeff_2;
	}
	public ArrayList<Integer> getVisited() {
		return visited;
	}
	public void setVisited(ArrayList<Integer> visited) {
		this.visited = visited;
	}

}
