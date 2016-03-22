package org.lri.oak.topks;

import java.util.Arrays;
import java.util.List;

import org.dbweb.socialsearch.shared.Params;
import org.dbweb.socialsearch.topktrust.algorithm.TopKAlgorithm;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathMultiplication;
import org.dbweb.socialsearch.topktrust.algorithm.paths.OptimalPaths;
import org.dbweb.socialsearch.topktrust.algorithm.score.Score;
import org.dbweb.socialsearch.topktrust.algorithm.score.TfIdfScore;
import org.junit.Assert;
import org.junit.Test;

public class YelpTests {

  private TopKAlgorithm algo;

  public YelpTests() {
    Params.TEST = true;
    // Parameters test dataset
    Params.dir = System.getProperty("user.dir") + "/test/yelp/TOPZIP/small/";
    Params.networkFile = "network.txt";
    Params.triplesFile = "triples.txt";
    Params.ILFile = "tag-inverted.txt";
    Params.threshold = 0f;
    Score score = new TfIdfScore();
    OptimalPaths optpath = new OptimalPaths("network", true);
    this.algo = new TopKAlgorithm(score, 0f, new PathMultiplication(), optpath);
  }

  @Test
  public void testSearchQuery() {
    this.algo.setSkippedTests(1);

    String multiQuery = "chinese+restaur";
    List<String> query;
    query = Arrays.asList(multiQuery.split("\\+"));
    this.algo.executeQuery(1, query, 20, 0f, 2000, 30000);

    Assert.assertEquals(this.algo.getNumloops(), 2795);
  }

}
