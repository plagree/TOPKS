package org.dbweb.socialsearch.topktrust.algorithm.score;

public class TfIdfScore extends Score {

    @Override
    public float getScore(float tf, float idf) {
        return tf * idf;
    }

    @Override
    public String toString() {
        return "tfidf";
    }

}
