package org.dbweb.Arcomem.datastructures;

public class Interval {
	private  double lower;
	private  double upper;
	
	public Interval(){
		super();
	}
	
	
	public Interval(double lower, double upper) {
		super();
		this.lower = lower;
		this.upper = upper;
	}
	public double getLower() {
		return lower;
	}
	public void setLower(double lower) {
		this.lower = lower;
	}
	public double getUpper() {
		return upper;
	}
	public void setUpper(double upper) {
		this.upper = upper;
	}
	
	public void print() {
		System.out.println("["+this.lower+","+this.upper+"]");
	}
	//TODO interval functions and sampling

	public String getString() {
		return "["+String.valueOf(lower)+","+String.valueOf(upper)+"]";
	}
	
}
