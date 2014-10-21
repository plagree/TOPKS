package org.dbweb.socialsearch.topktrust.algorithm.score;

public abstract class Score {
	public abstract double getScore(double tf, double idf);
	public abstract String toString();
}
