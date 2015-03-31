package org.dbweb.socialsearch.topktrust.algorithm.score;

public class TfIdfScore extends Score {
	@Override
	public double getScore(double tf, double idf) {
		return tf*idf;
	}

	@Override
	public String toString() {
		return "tfidf";
	}

}
