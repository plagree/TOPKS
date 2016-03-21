package org.lri.oak.topks;

import java.util.ArrayList;
import java.util.List;

import org.dbweb.socialsearch.shared.Params;
import org.dbweb.socialsearch.topktrust.algorithm.TopKAlgorithm;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathMultiplication;
import org.dbweb.socialsearch.topktrust.algorithm.paths.OptimalPaths;
import org.dbweb.socialsearch.topktrust.algorithm.score.Score;
import org.dbweb.socialsearch.topktrust.algorithm.score.TfIdfScore;
import org.dbweb.socialsearch.topktrust.datastructure.Item;
import org.junit.Assert;
import org.junit.Test;

public class TopKAlgorithmTest {

  // TODO: chooseBranch, reset, incremental

  private static final float DELTA = 1e-7f;
  private TopKAlgorithm algo;

  public TopKAlgorithmTest() {
    // Parameters test dataset
    Params.dir = System.getProperty("user.dir") + "/test/test/";
    Params.number_documents = 6;
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
    this.algo.setSkippedTests(20);  // Go to the end

    // Query q="style" by seeker s=1 with skippedTests=20 (visit the whole graph)
    List<String> query = new ArrayList<String>();
    query.add("style");
    this.algo.executeQuery(1, query, 2, 0f, 200, 200);

    Assert.assertEquals(this.algo.getNumloops(), 10);

    List<Item> results = this.algo.getTopk(2);
    // First item for q="style": item 6, socialScore log(2) * 0.9 (idf(q) = log(6 / 2))
    // textualScore = log(2) * 1 (only user 2 tagged item 6 with tag "style")
    Assert.assertEquals(results.get(0).getItemId(), 6l);
    System.out.println(results.get(0).getSocialScore());
    Assert.assertEquals(results.get(0).getSocialScore(),
            (float)(Math.log(2) * 0.9), DELTA);
    Assert.assertEquals(results.get(0).getTextualScore(),
            (float)(Math.log(2) * 1), DELTA);

    // Second item for q="style": item 4, socialScore log(2) * 0.7782
    // textualScore = log(2) * 3 (3 users tagged item 4 with tag "style")
    Assert.assertEquals(results.get(1).getItemId(), 4l);
    Assert.assertEquals(results.get(1).getSocialScore(),
            (float)(Math.log(2) * 0.7782), DELTA); // only user 3 is visited
    Assert.assertEquals(results.get(1).getTextualScore(),
            (float)(Math.log(2) * 3), DELTA);
  }

  @Test
  public void testTerminationCondition() {
    // We test the termination condition of the TOPKS algorithm at each new visited user
    this.algo.setSkippedTests(1);

    // Query q="style" by seeker s=1 with skippedTests=1 (termination test at every iteration)
    List<String> query = new ArrayList<String>();
    query.add("style");
    this.algo.executeQuery(1, query, 2, 0f, 200, 200);

    Assert.assertEquals(this.algo.getNumloops(), 4);

    List<Item> results = this.algo.getTopk(2);
    // First item for q="style": item 6, socialScore log(2) * 0.9 ( idf(q) = log(6 / 2))
    // textualScore = log(2) * 1 ( only user 2 tagged item 6 with tag "style" )
    Assert.assertEquals(results.get(0).getItemId(), 6l);
    Assert.assertEquals(results.get(0).getSocialScore(),
            (float)(Math.log(2) * 0.9), DELTA);
    Assert.assertEquals(results.get(0).getTextualScore(),
            (float)(Math.log(2) * 1), DELTA);

    // Second item for q="style": item 4, socialScore log(2) * 0.7782
    // textualScore = log(2) * 3 ( 3 users tagged item 4 with tag "style" )
    Assert.assertEquals(results.get(1).getItemId(), 4l);
    Assert.assertEquals(results.get(1).getSocialScore(),
            (float)(Math.log(2) * 0.6), DELTA); // only user 3 is visited
    Assert.assertEquals(results.get(1).getTextualScore(),
            (float)(Math.log(2) * 3), DELTA);
  }

  @Test
  public void testPrefix() {
    this.algo.setSkippedTests(15);
    // Query q="g" by seeker s=1 with skippedTests=15 (no termination test)
    List<String> query = new ArrayList<String>();
    query.add("g");
    int seeker = 1;
    int k = 5;
    int t = 200;
    int nNeigh = 200;
    float alpha = 0f;

    this.algo.executeQuery(seeker, query, k, alpha, t, nNeigh);

    Assert.assertEquals(this.algo.getNumloops(), 10);

    List<Item> results = this.algo.getTopk(2);

    // id: 6 completion: "glasses"
    Assert.assertEquals(results.get(0).getItemId(), 6);
    Assert.assertEquals(results.get(0).getCompletion(), "glasses");
    Assert.assertEquals(results.get(0).getSocialScore(),
            (float)1.6479185, DELTA);
    Assert.assertEquals(results.get(0).getTextualScore(),
            (float)2.1972246, DELTA);

    // id: 4 completion: "goth"
    Assert.assertEquals(results.get(1).getItemId(), 4);
    Assert.assertEquals(results.get(1).getCompletion(), "goth");
    Assert.assertEquals(results.get(1).getSocialScore(),
            (float)Math.log(6 / 2) * 0.4212, DELTA);
    Assert.assertEquals(results.get(1).getTextualScore(),
            (float)Math.log(6 / 2) * 2, DELTA);

    // id: 1 completion: "gloomy"
    Assert.assertEquals(results.get(2).getItemId(), 1);
    Assert.assertEquals(results.get(2).getCompletion(), "gloomy");
    Assert.assertEquals(results.get(2).getSocialScore(),
            (float)Math.log(6 / 2) * 0.405, DELTA);
    Assert.assertEquals(results.get(2).getTextualScore(),
            (float)Math.log(6 / 2) * 1, DELTA);

    // id: 2 completion: "grunge"
    Assert.assertEquals(results.get(3).getItemId(), 2);
    Assert.assertEquals(results.get(3).getCompletion(), "grunge");
    Assert.assertEquals(results.get(3).getSocialScore(),
            (float)Math.log(6f / 5) * 1.491, DELTA);
    Assert.assertEquals(results.get(3).getTextualScore(),
            (float)Math.log(6f / 5) * 3, DELTA);

    // id: 5 completion: "grunge"
    Assert.assertEquals(results.get(4).getItemId(), 5);
    Assert.assertEquals(results.get(4).getCompletion(), "grunge");
    Assert.assertEquals(results.get(4).getSocialScore(),
            (float)Math.log(6f / 5) * 0.162, DELTA);
    Assert.assertEquals(results.get(4).getTextualScore(),
            (float)Math.log(6f / 5) * 1, DELTA);
  }

  /**
   * Used to test both prefix early top-k stopping and multiple words
   */
  @Test
  public void testMultipleWords() {
    this.algo.setSkippedTests(1);
    // Query q="g" by seeker s=1 with skippedTests=15 (no termination test)
    List<String> query = new ArrayList<String>();
    query.add("grunge");
    query.add("g");
    int seeker = 1, k = 2, t = 200, nNeigh = 20;
    float alpha = 0f;

    this.algo.executeQuery(seeker, query, k, alpha, t, nNeigh);
    for (Item e: this.algo.getCandidates().getListTopk(5)) {
      System.out.println(e);
      System.out.println(e.getItemId()+", "+e.getComputedWorstScore());
      e.computeWorstScore();
      System.out.println(e.getItemId()+", "+e.getComputedWorstScore());
    }

    Assert.assertEquals(this.algo.getNumloops(), 10);
  }

  @Test
  public void testAlphaNonZero() {
    this.algo.setSkippedTests(1);
    // Query q="style" by seeker s=8 with skippedTests=1 (termination test at every iteration)
    List<String> query = new ArrayList<String>();
    query.add("style");
    this.algo.executeQuery(8, query, 5, 0.44444444f, 100, 200);

    List<Item> results = this.algo.getTopk(2);

    Assert.assertEquals(results.get(0).getItemId(), 4l);
    Assert.assertEquals(results.get(0).getSocialScore(), 
            (float)(Math.log(2) * 0.124), DELTA);
    Assert.assertEquals(results.get(0).getTextualScore(),
            (float)(Math.log(2) * 3), DELTA);

    Assert.assertEquals(results.get(1).getItemId(), 2l);
    Assert.assertEquals(results.get(1).getSocialScore(),
            (float)(Math.log(2) * 0.02), DELTA); // only user 3 is visited
    Assert.assertEquals(results.get(1).getTextualScore(),
            (float)(Math.log(2) * 1), DELTA);
  }

}
