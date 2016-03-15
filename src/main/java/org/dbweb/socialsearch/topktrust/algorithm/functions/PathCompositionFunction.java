package org.dbweb.socialsearch.topktrust.algorithm.functions;

/**
 *
 * @author Silver
 */
public abstract class PathCompositionFunction<T extends Comparable<T>> {

  protected double coeff = 0;
  public abstract T compute(T term1, T term2);
  public abstract T inverse(T val);
  public abstract String toString();

  public void setCoeff(double coeff){
    this.coeff = 0;
  }

  public double getCoeff(){
    return coeff;
  }

}