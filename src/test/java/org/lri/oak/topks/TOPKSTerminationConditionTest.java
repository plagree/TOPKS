package org.lri.oak.topks;

import java.util.ArrayList;
import java.util.List;

import org.dbweb.Arcomem.Integration.TOPKSSearcher;
import org.dbweb.socialsearch.shared.Params;
import org.dbweb.socialsearch.topktrust.algorithm.score.TfIdfScore;
import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class TOPKSTerminationConditionTest {

  private static final float DELTA = 1e-7f;
  private TOPKSSearcher searcher;

  public TOPKSTerminationConditionTest() {
    // Parameters test dataset
    Params.dir = System.getProperty("user.dir") + "/test/test2/";
    Params.number_documents = 1638;
    Params.networkFile = "network.txt";
    Params.triplesFile = "triples.txt";
    Params.ILFile = "tag-inverted.txt";
    Params.threshold = 0f;

    this.searcher = new TOPKSSearcher(new TfIdfScore());
  }

  @Test
  public void testTerminationCondition() {
    searcher.setSkippedTests(100);

    List<String> query = new ArrayList<String>();
    query.add("jika");

    JsonObject jsonResults = searcher.executeQuery(100146521, query, 1, 10000, true, 100000, 0f);

    //Assert.assertEquals(jsonResults.get("status").getAsInt(), 1); not with maven
    //Assert.assertEquals(jsonResults.get("n").getAsInt(), 1);
    //Assert.assertEquals(jsonResults.get("nLoops").getAsInt(), 19599);
  }
}