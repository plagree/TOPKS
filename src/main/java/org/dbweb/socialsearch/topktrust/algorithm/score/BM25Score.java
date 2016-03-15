package org.dbweb.socialsearch.topktrust.algorithm.score;

public class BM25Score extends Score {
    private float k1 = 3;

    public void setK1(float k1){
        this.k1 = k1;
    }
    @Override
    public float getScore(float tf, float idf) {
        return idf * (k1 + 1) * tf / (tf + k1);
    }
    @Override
    public String toString() {
        return "bm25";
    }

}