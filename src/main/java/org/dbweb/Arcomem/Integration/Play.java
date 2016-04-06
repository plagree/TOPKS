package org.dbweb.Arcomem.Integration;

import org.dbweb.socialsearch.shared.Params;
import org.dbweb.socialsearch.topktrust.algorithm.TopKAlgorithm;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathMultiplication;
import org.dbweb.socialsearch.topktrust.algorithm.paths.OptimalPaths;
import org.dbweb.socialsearch.topktrust.algorithm.score.Score;
import org.dbweb.socialsearch.topktrust.algorithm.score.TfIdfScore;

public class Play {
  
  private static final int N_EXPERIMENTS = 100000;

  public static void main(String[] args) {
    Params.dir = System.getProperty("user.dir") + "/test/yelp/TOPZIP/small/";
    Params.networkFile = "network.txt";
    Params.triplesFile = "triples.txt";
    // Index files and load data in memory
    Score score = new TfIdfScore();
    OptimalPaths optpath = new OptimalPaths("network", true);
    TopKAlgorithm algo = new TopKAlgorithm(score, 0f, new PathMultiplication(), optpath);
    // Experiment IL fast read
    long fast_il = algo.fast_il(N_EXPERIMENTS);
    System.out.println((float)fast_il);
    
    // Experiment complete IL read
    long complete_il = algo.complete_il(N_EXPERIMENTS);
    System.out.println((float)complete_il);
    
    // Experiment P-SPACE READ
    long p_space = algo.p_space(N_EXPERIMENTS);
    System.out.println((float)p_space);
  }

}
