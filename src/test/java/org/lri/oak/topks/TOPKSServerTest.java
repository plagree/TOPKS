package org.lri.oak.topks;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.dbweb.Arcomem.Integration.TOPKSSearcher;
import org.dbweb.Arcomem.Integration.TOPKSServer;
import org.dbweb.socialsearch.shared.Params;
import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class  TOPKSServerTest {

    private static final float DELTA = 1e-7f;
    private static final int PORT = 8000;
    
    @Test
    public void testServerReceiveJSON() {    	
    	// Parameters dataset
    	Params.dir = System.getProperty("user.dir")+"/test/";
		Params.number_documents = 6;
		Params.networkFile = "network.txt";
		Params.triplesFile = "triples.txt";
		Params.ILFile = "tag-inverted.txt";
		Params.tagFreqFile = "tag-freq.txt";
		Params.threshold = 0f;
		
		TOPKSSearcher searcher = new TOPKSSearcher();
    	TOPKSServer server = new TOPKSServer(searcher);
    	server.run();
    	
    	// Prepare arguments for query
    	String q = "style";
    	int k = 2;
    	long seeker = 1l;
    	int t = 200;
    	boolean newQuery = true;
    	int nNeigh = 1000;
    	float alpha = 0f;
    	
    	// Create the URL
    	String url = "http://localhost:"+PORT+"/topks?q="+q+"&seeker="+seeker+"&t="+t+
    			"&newQuery="+newQuery+"&nNeigh="+nNeigh+"&alpha="+alpha+"&k="+k;
    	URL obj;
		try {
			obj = new URL(url);
			HttpURLConnection request = (HttpURLConnection) obj.openConnection();
		    request.connect();
			
		    JsonParser jp = new JsonParser(); //from gson
		    JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent())); //convert the input stream to a json element
		    JsonObject rootobj = root.getAsJsonObject();
		    Assert.assertEquals(rootobj.get("status").getAsInt(), 1);
		    Assert.assertEquals(rootobj.get("n").getAsInt(), 2);
		    JsonArray results = rootobj.get("results").getAsJsonArray();
		    System.out.println(results.toString());
		    
		    // First item for q="style": item 6, socialScore log(2) * 0.9 ( idf(q) = log(6 / 2) )
		    // textualScore = log(2) * 1 ( only user 2 tagged item 6 with tag "style" )
		    Assert.assertEquals(results.get(0).getAsJsonObject().get("id").getAsLong(), 6l);
		    Assert.assertEquals(results.get(0).getAsJsonObject().get("socialScore").getAsFloat(), (float)(Math.log(2) * 0.9), DELTA);
		    Assert.assertEquals(results.get(0).getAsJsonObject().get("textualScore").getAsFloat(), (float)(Math.log(2) * 1), DELTA);
		    
		    // Second item for q="style": item 4, socialScore log(2) * 0.7782
		    // textualScore = log(2) * 3 ( 3 users tagged item 4 with tag "style" )
		    Assert.assertEquals(results.get(1).getAsJsonObject().get("id").getAsLong(), 4l);
		    Assert.assertEquals(results.get(1).getAsJsonObject().get("socialScore").getAsFloat(), (float)(Math.log(2) * 0.7782), DELTA);
		    Assert.assertEquals(results.get(1).getAsJsonObject().get("textualScore").getAsFloat(), (float)(Math.log(2) * 3), DELTA);
		    
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

}

