package org.dbweb.socialsearch.topktrust.algorithm.functions;

public class PathOne extends PathCompositionFunction<Float> {

	@Override
	public Float compute(Float term1, Float term2) {
		// TODO Auto-generated method stub
		return 1.0f;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "path_one";
	}

	@Override
	public Float inverse(Float val) {
		return 1.0f;
	}

}
