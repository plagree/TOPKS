package org.dbweb.socialsearch.topktrust.algorithm.score;

public abstract class Score {
    public abstract float getScore(float tf, float idf);
    public abstract String toString();
}