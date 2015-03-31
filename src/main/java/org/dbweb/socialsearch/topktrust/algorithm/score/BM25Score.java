package org.dbweb.socialsearch.topktrust.algorithm.score;

public class BM25Score extends Score {
	private double k1 = 3.0;
	public void setK1(double k1){
		this.k1 = k1;
	}
	@Override
	public double getScore(double tf, double idf) {
		return idf*(k1+1)*tf/(tf+k1);
	}
	@Override
	public String toString() {
		return "bm25";
	}

}
