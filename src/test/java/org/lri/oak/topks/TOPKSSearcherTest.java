package org.lri.oak.topks;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dbweb.Arcomem.Integration.TOPKSSearcher;
import org.dbweb.socialsearch.shared.Params;
import org.dbweb.socialsearch.topktrust.algorithm.score.TfIdfScore;
import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class TOPKSSearcherTest {
	
	private static final float DELTA = 1e-7f;
	private TOPKSSearcher searcher;
	
	public TOPKSSearcherTest() {
		// Parameters test dataset
    	Params.dir = System.getProperty("user.dir")+"/test/test/";
		Params.number_documents = 6;
		Params.networkFile = "network.txt";
		Params.triplesFile = "triples.txt";
		Params.ILFile = "tag-inverted.txt";
		Params.tagFreqFile = "tag-freq.txt";
		Params.threshold = 0f;
		
		searcher = new TOPKSSearcher(new TfIdfScore());
	}

	@Test
	public void testSearchQuery() {

		searcher.setSkippedTests(20); // We test the termination condition of the TOPKS algorithm at each new visited user
			
		// Query q="style" by seeker s=1 with skippedTests=20 (visit the whole graph)
		List<String> query = new ArrayList<String>();
		query.add("style");
		
		try {
			JsonObject jsonResults = searcher.executeQuery("1", query, 2, 200, true, 200, 0f);
			
			Assert.assertEquals(jsonResults.get("status").getAsInt(), 1);
		    Assert.assertEquals(jsonResults.get("n").getAsInt(), 2);
		    Assert.assertEquals(jsonResults.get("nLoops").getAsInt(), 10);
		    JsonArray results = jsonResults.get("results").getAsJsonArray();
		    
		    // First item for q="style": item 6, socialScore log(2) * 0.9 ( idf(q) = log(6 / 2) )
		    // textualScore = log(2) * 1 ( only user 2 tagged item 6 with tag "style" )
		    Assert.assertEquals(results.get(0).getAsJsonObject().get("id").getAsLong(), 6l);
		    Assert.assertEquals(results.get(0).getAsJsonObject().get("socialScore").getAsFloat(), (float)(Math.log(2) * 0.9), DELTA);
		    Assert.assertEquals(results.get(0).getAsJsonObject().get("textualScore").getAsFloat(), (float)(Math.log(2) * 1), DELTA);
		    
		    // Second item for q="style": item 4, socialScore log(2) * 0.7782
		    // textualScore = log(2) * 3 ( 3 users tagged item 4 with tag "style" )
		    Assert.assertEquals(results.get(1).getAsJsonObject().get("id").getAsLong(), 4l);
		    Assert.assertEquals(results.get(1).getAsJsonObject().get("socialScore").getAsFloat(),
		    					(float)(Math.log(2) * 0.7782), DELTA); // only user 3 is visited
		    Assert.assertEquals(results.get(1).getAsJsonObject().get("textualScore").getAsFloat(), (float)(Math.log(2) * 3), DELTA);
			
		} catch (SQLException e) {
			Assert.fail();
			e.printStackTrace();
		}
	}
	
	@Test
	public void testTerminationCondition() {

		searcher.setSkippedTests(1); // We test the termination condition of the TOPKS algorithm at each new visited user
		
		// Query q="style" by seeker s=1 with skippedTests=1 (termination test at every iteration)
		List<String> query = new ArrayList<String>();
		query.add("style");
		
		try {
			JsonObject jsonResults = searcher.executeQuery("1", query, 2, 200, true, 200, 0f);
			
			Assert.assertEquals(jsonResults.get("status").getAsInt(), 1);
		    Assert.assertEquals(jsonResults.get("n").getAsInt(), 2);
		    Assert.assertEquals(jsonResults.get("nLoops").getAsInt(), 4);
		    JsonArray results = jsonResults.get("results").getAsJsonArray();
		    
		    // First item for q="style": item 6, socialScore log(2) * 0.9 ( idf(q) = log(6 / 2) )
		    // textualScore = log(2) * 1 ( only user 2 tagged item 6 with tag "style" )
		    Assert.assertEquals(results.get(0).getAsJsonObject().get("id").getAsLong(), 6l);
		    Assert.assertEquals(results.get(0).getAsJsonObject().get("socialScore").getAsFloat(), (float)(Math.log(2) * 0.9), DELTA);
		    Assert.assertEquals(results.get(0).getAsJsonObject().get("textualScore").getAsFloat(), (float)(Math.log(2) * 1), DELTA);
		    
		    // Second item for q="style": item 4, socialScore log(2) * 0.7782
		    // textualScore = log(2) * 3 ( 3 users tagged item 4 with tag "style" )
		    Assert.assertEquals(results.get(1).getAsJsonObject().get("id").getAsLong(), 4l);
		    Assert.assertEquals(results.get(1).getAsJsonObject().get("socialScore").getAsFloat(),
		    					(float)(Math.log(2) * 0.6), DELTA); // only user 3 is visited
		    Assert.assertEquals(results.get(1).getAsJsonObject().get("textualScore").getAsFloat(), (float)(Math.log(2) * 3), DELTA);
			
		} catch (SQLException e) {
			Assert.fail();
			e.printStackTrace();
		}
	}
	
	@Test
	public void testPrefix() {

		searcher.setSkippedTests(15);
		
		// Query q="g" by seeker s=1 with skippedTests=15 (no termination test)
		List<String> query = new ArrayList<String>();
		query.add("g");
		String seeker = "1";
		int k = 5;
		int t = 200;
		boolean newQuery = true;
		int nNeigh = 200;
		float alpha = 0f;
		
		try {
			JsonObject jsonResults = searcher.executeQuery(seeker, query, k, t, newQuery, nNeigh, alpha);
			
			Assert.assertEquals(jsonResults.get("status").getAsInt(), 1);
		    Assert.assertEquals(jsonResults.get("n").getAsInt(), 5);
		    Assert.assertEquals(jsonResults.get("nLoops").getAsInt(), 10);
		    JsonArray results = jsonResults.get("results").getAsJsonArray();
		    
		    // id: 6 completion: "glasses"
		    Assert.assertEquals(results.get(0).getAsJsonObject().get("id").getAsInt(), 6);
		    Assert.assertEquals(results.get(0).getAsJsonObject().get("rank").getAsInt(), 1);
		    Assert.assertEquals(results.get(0).getAsJsonObject().get("completion").getAsString(), "glasses");
		    Assert.assertEquals(results.get(0).getAsJsonObject().get("socialScore").getAsFloat(), (float)1.6479185, DELTA);
		    Assert.assertEquals(results.get(0).getAsJsonObject().get("textualScore").getAsFloat(), (float)2.1972246, DELTA);
		    
		    // id: 4 completion: "goth"
		    Assert.assertEquals(results.get(1).getAsJsonObject().get("id").getAsInt(), 4);
		    Assert.assertEquals(results.get(1).getAsJsonObject().get("rank").getAsInt(), 2);
		    Assert.assertEquals(results.get(1).getAsJsonObject().get("completion").getAsString(), "goth");
		    Assert.assertEquals(results.get(1).getAsJsonObject().get("socialScore").getAsFloat(), (float)Math.log( 6 / 2 ) * 0.4212, DELTA);
		    Assert.assertEquals(results.get(1).getAsJsonObject().get("textualScore").getAsFloat(), (float)Math.log( 6 / 2 ) * 2, DELTA);
			
		    // id: 1 completion: "gloomy"
		    Assert.assertEquals(results.get(2).getAsJsonObject().get("id").getAsInt(), 1);
		    Assert.assertEquals(results.get(2).getAsJsonObject().get("rank").getAsInt(), 3);
		    Assert.assertEquals(results.get(2).getAsJsonObject().get("completion").getAsString(), "gloomy");
		    Assert.assertEquals(results.get(2).getAsJsonObject().get("socialScore").getAsFloat(), (float)Math.log( 6 / 2 ) * 0.405, DELTA);
		    Assert.assertEquals(results.get(2).getAsJsonObject().get("textualScore").getAsFloat(), (float)Math.log( 6 / 2 ) * 1, DELTA);
			
		    // id: 2 completion: "grunge"
		    Assert.assertEquals(results.get(3).getAsJsonObject().get("id").getAsInt(), 2);
		    Assert.assertEquals(results.get(3).getAsJsonObject().get("rank").getAsInt(), 4);
		    Assert.assertEquals(results.get(3).getAsJsonObject().get("completion").getAsString(), "grunge");
		    Assert.assertEquals(results.get(3).getAsJsonObject().get("socialScore").getAsFloat(), (float)Math.log( 6f / 5 ) * 1.491, DELTA);
		    Assert.assertEquals(results.get(3).getAsJsonObject().get("textualScore").getAsFloat(), (float)Math.log( 6f / 5 ) * 3, DELTA);
		    
		    // id: 5 completion: "grunge"
		    Assert.assertEquals(results.get(4).getAsJsonObject().get("id").getAsInt(), 5);
		    Assert.assertEquals(results.get(4).getAsJsonObject().get("rank").getAsInt(), 5);
		    Assert.assertEquals(results.get(4).getAsJsonObject().get("completion").getAsString(), "grunge");
		    Assert.assertEquals(results.get(4).getAsJsonObject().get("socialScore").getAsFloat(), (float)Math.log( 6f / 5 ) * 0.162, DELTA);
		    Assert.assertEquals(results.get(4).getAsJsonObject().get("textualScore").getAsFloat(), (float)Math.log( 6f / 5 ) * 1, DELTA);
			
		} catch (SQLException e) {
			Assert.fail();
			e.printStackTrace();
		}
	}

	//TODO test on multiple words
	
	@Test
	public void testAlphaNonZero() {
		
		searcher.setSkippedTests(1); // We test the termination condition of the TOPKS algorithm at each new visited user
		
		//Query q="style" by seeker s=8 with skippedTests=1 (termination test at every iteration)
		List<String> query = new ArrayList<String>();
		query.add("style");
		
		try {
			JsonObject jsonResults = searcher.executeQuery("8", query, 5, 100, true, 200, 0.44444444f);
			System.out.println(jsonResults.toString());
			Assert.assertEquals(jsonResults.get("status").getAsInt(), 1);
		    Assert.assertEquals(jsonResults.get("n").getAsInt(), 3);
		    //Assert.assertEquals(jsonResults.get("nLoops").getAsInt(), 4);
		    JsonArray results = jsonResults.get("results").getAsJsonArray();
		    
		    Assert.assertEquals(results.get(0).getAsJsonObject().get("id").getAsLong(), 4l);
		    Assert.assertEquals(results.get(0).getAsJsonObject().get("socialScore").getAsFloat(), (float)(Math.log(2) * 0.124), DELTA);
		    Assert.assertEquals(results.get(0).getAsJsonObject().get("textualScore").getAsFloat(), (float)(Math.log(2) * 3), DELTA);
		    
		    Assert.assertEquals(results.get(1).getAsJsonObject().get("id").getAsLong(), 2l);
		    Assert.assertEquals(results.get(1).getAsJsonObject().get("socialScore").getAsFloat(),
		    					(float)(Math.log(2) * 0.02), DELTA); // only user 3 is visited
		    Assert.assertEquals(results.get(1).getAsJsonObject().get("textualScore").getAsFloat(), (float)(Math.log(2) * 1), DELTA);
			
		} catch (SQLException e) {
			Assert.fail();
			e.printStackTrace();
		}
	}
	
}
