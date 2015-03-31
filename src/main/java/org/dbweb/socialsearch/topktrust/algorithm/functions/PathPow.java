package org.dbweb.socialsearch.topktrust.algorithm.functions;

import java.util.Locale;

public class PathPow extends PathCompositionFunction<Float> {
	
	public PathPow(){
		this.coeff = 2.0f;
	}
	
	public PathPow(double coeff){
		this.coeff = coeff;
	}
	
	@Override
	public Float compute(Float term1, Float term2) {
		return (float) (term1*Math.pow(coeff, -(1.0f/term2)));
	}

	@Override
	public String toString() {
		return String.format(Locale.US,"path_pow%f",coeff);
	}

	@Override
	public Float inverse(Float val) {
		return (float) Math.pow(coeff, 1.0f/val);
	}
	
	@Override
    public void setCoeff(double coeff){
    	this.coeff = coeff;
    }

}
