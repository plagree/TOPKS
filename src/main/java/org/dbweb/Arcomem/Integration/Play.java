package org.dbweb.Arcomem.Integration;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.dbweb.socialsearch.shared.Params;
import org.dbweb.socialsearch.topktrust.algorithm.TopKAlgorithm;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathMultiplication;
import org.dbweb.socialsearch.topktrust.algorithm.paths.OptimalPaths;
import org.dbweb.socialsearch.topktrust.algorithm.score.Score;
import org.dbweb.socialsearch.topktrust.algorithm.score.TfIdfScore;

public class Play {

  private static final int N_EXPERIMENTS = 1;

  public static void main(String[] args) {
    Params.dir = "/home/lagree/datasets/yelp/CIKM/big/100/";
    Params.networkFile = "network.txt";
    Params.triplesFile = "triples100.txt";
    // Index files and load data in memory
    Score score = new TfIdfScore();
    OptimalPaths optpath = new OptimalPaths("network", true);
    TopKAlgorithm algo = new TopKAlgorithm(score, 0f, new PathMultiplication(), optpath);
    PrintWriter writer;
    try {
      writer = new PrintWriter("ndcg.csv", "UTF-8");
      for (int j = 0; j < 10; j++) {
        List<Float> ndcgDistribution = algo.userSequenceDistribution();
        for (int i = 0; i < ndcgDistribution.size() - 1; i++)
          writer.print(ndcgDistribution.get(i)+",");
        writer.print(ndcgDistribution.get(ndcgDistribution.size()-1)+"\n");
      }
      writer.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    System.exit(1);

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
