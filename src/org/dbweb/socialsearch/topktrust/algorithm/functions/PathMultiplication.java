/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbweb.socialsearch.topktrust.algorithm.functions;

/**
 *
 * @author Silver
 */
public class PathMultiplication extends PathCompositionFunction<Float> {

    @Override
	public Float compute(Float term1, Float term2) {
        return term1*term2;
    }

	@Override
	public String toString() {
		return "path_mult";
	}

	@Override
	public Float inverse(Float val) {
		return (float) 1.0f/val;
	}

}
