package org.dbweb.Arcomem.Integration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dbweb.socialsearch.shared.Params;
import org.dbweb.socialsearch.topktrust.algorithm.TopKAlgorithm;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathCompositionFunction;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathMultiplication;
import org.dbweb.socialsearch.topktrust.algorithm.paths.OptimalPaths;
import org.dbweb.socialsearch.topktrust.algorithm.score.Score;
import org.dbweb.socialsearch.topktrust.algorithm.score.TfIdfScore;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class TOPKSSearcher {
	
	private static final boolean heap = true;
	@SuppressWarnings("rawtypes")
	private static final PathCompositionFunction pathFunction = new PathMultiplication();
	private static double coeff = 2.0f;
	public static final String network = "soc_snet_dt";
	public static final String taggers = "soc_tag_80";
	private static final int method = 1;
	private TopKAlgorithm topk_alg;

	public TOPKSSearcher(Score score) {
		
		OptimalPaths optpath;
		try {
			optpath = new OptimalPaths(network, null, heap, null, coeff);
			topk_alg = new TopKAlgorithm(null, taggers, network, method, score, 0f, pathFunction, optpath, 1);
		}
		catch (SQLException ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param k number maximum of returned results
	 * @param t time given to compute an answer
	 * @param user id of the seeker
	 * @param newQuery true if new query, false if incremental query
	 * @param nNeigh 
	 * @return JsonObject containing the lower and upper score for every returned item
	 * @throws SQLException 
	 */
	public JsonObject executeQuery(String user, List<String> query, int k, int t, boolean newQuery, int nNeigh, float alpha) throws SQLException {
		topk_alg.setAlpha(alpha);
		topk_alg.executeQuery(user, query, k, t, newQuery, nNeigh);
		String[] words = new String[query.size()];
		int i = 0;
		for (String term: query) {
			words[i] = term;
			i++;
		}
		topk_alg.reinitialize(words, 1);
		JsonObject jsonResult = topk_alg.getJsonAnswer(k);
		return jsonResult;
	}
	
	public JsonObject executeQueryNDCG(String user, List<String> query, int k, int t, boolean newQuery, int nNeigh, float alpha) throws SQLException {
		// Computation for infinity (oracle)
		Params.NDCG = false;
		topk_alg.setAlpha(alpha);
		topk_alg.executeQuery(user, query, k, t, newQuery, nNeigh);
		topk_alg.computeOracleNDCG(k);
		String[] words = new String[query.size()];
		int i = 0;
		for (String term: query) {
			words[i] = term;
			i++;
		}
		topk_alg.reinitialize(words, 1);
		
		// Computation of the NDCGResults object (NDCG vs t)
		Params.NDCG = true;
		topk_alg.executeQuery(user, query, k, t, newQuery, nNeigh);
		System.out.println(topk_alg.getJsonAnswer(k).toString());
		JsonObject jsonResult = topk_alg.getJsonNDCG(k);
		Params.NDCG = false;
		return jsonResult;
	}
	
	public JsonObject executeIncrementalQuery(String user, List<String> query, int k, int t, int nNeigh, float alpha, int lengthPrefixMin) throws SQLException {
		topk_alg.setAlpha(alpha);
		JsonArray arrayResults = new JsonArray();
		JsonObject currResult = null;
		int lengthTag = 0, nbSeenWords = 0;
		List<String> currQuery = new ArrayList<String>();
		
		for (String word: query) {
			lengthTag = word.length();
			nbSeenWords++;
			for (int l=lengthPrefixMin; l<=lengthTag; l++) {
				
				// true for the first query execution
				boolean newQuery = true;
				// New word
				if (l == lengthPrefixMin) {
					currQuery.add(word.substring(0, l));
					topk_alg.executeQuery(user, currQuery, k, t, newQuery, nNeigh);
					newQuery = false;
				}
				
				// Incremental computation
				else {
					currQuery.remove(nbSeenWords-1);
					currQuery.add(word.substring(0, l));
					topk_alg.executeQueryPlusLetter(user, currQuery, l, t);
				}
				currResult = new JsonObject();
				currResult.add("l",  new JsonPrimitive(l));
				currResult.add("word", new JsonPrimitive(word));
				currResult.add("topksResult", topk_alg.getJsonAnswer(k));
				arrayResults.add(currResult);
			}
			break; // Just one word so far
		}
		// Clean the TOPKSAlgorithm object
		String[] words = new String[query.size()];
		int i = 0;
		for (String term: query) {
			words[i] = term;
			i++;
		}
		topk_alg.reinitialize(words, lengthPrefixMin);
		
		JsonObject jsonResult = new JsonObject();
		jsonResult.add("results", arrayResults);
		return jsonResult;
	}

	public void setSkippedTests(int skippedTests) {
		this.topk_alg.setSkippedTests(skippedTests);
	}
	
}
