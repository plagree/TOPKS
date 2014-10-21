package org.dbweb.socialsearch.topktrust.algorithm.functions;

public class PathMinimum extends PathCompositionFunction<Float> {

	@Override
	public Float compute(Float term1, Float term2) {
		if(term1<term2) return term1;
		else return term2;
	}

	@Override
	public String toString() {
		return "path_min";
	}

	@Override
	public Float inverse(Float val) {
		return null;
	}
	
}
