package org.dbweb.Arcomem.Integration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

	public JsonObject executeQueryNDCG_vs_time(String user, List<String> query, int k, int t, boolean newQuery, int nNeigh, float alpha) throws SQLException {
		// Computation for infinity (oracle)
		Params.NDCG_TIME = false;
		topk_alg.setAlpha(alpha);
		topk_alg.executeQuery(user, query, k, 25000, newQuery, nNeigh);
		topk_alg.computeOracleNDCG(k);
		String[] words = new String[query.size()];
		int i = 0;
		for (String term: query) {
			words[i] = term;
			i++;
		}
		topk_alg.reinitialize(words, 1);

		// Computation of the NDCGResults object (NDCG vs t)
		Params.NDCG_TIME = true;
		topk_alg.executeQuery(user, query, k, t, newQuery, nNeigh);
		JsonObject jsonResult = topk_alg.getJsonNDCG_vs_time(k);
		topk_alg.reinitialize(words, 1);
		Params.NDCG_TIME = false;
		return jsonResult;
	}

	public JsonObject executeQueryNDCG_vs_nbusers(String user, List<String> query, int k, int t, boolean newQuery, int nNeigh, float alpha) throws SQLException {
		// Computation for infinity (oracle)
		Params.NDCG_USERS = false;
		topk_alg.setAlpha(alpha);
		topk_alg.executeQuery(user, query, k, 10000, newQuery, 100000);
		topk_alg.computeOracleNDCG(k);
		String[] words = new String[query.size()];
		int i = 0;
		for (String term: query) {
			words[i] = term;
			i++;
		}
		topk_alg.reinitialize(words, 1);
		// Computation of the NDCGResults object (NDCG vs t)
		Params.NDCG_USERS = true;
		topk_alg.executeQuery(user, query, k, t, newQuery, nNeigh);
		JsonObject jsonResult = topk_alg.getJsonNDCG_vs_nbusers(k);
		topk_alg.reinitialize(words, 1);
		Params.NDCG_USERS = false;
		return jsonResult;
	}

	public JsonObject executeQueryExactTopK_vs_time(String user, List<String> query, int k, boolean newQuery, float alpha) throws SQLException {
		// Computation for infinity (oracle)
		Params.EXACT_TOPK = false;
		topk_alg.setAlpha(alpha);
		topk_alg.executeQuery(user, query, k, 10000, newQuery, 100000);
		topk_alg.computeTopkInfinity(k);
		String[] words = new String[query.size()];
		int i = 0;
		for (String term: query) {
			words[i] = term;
			i++;
		}
		topk_alg.reinitialize(words, 1);
		// Computation for topk exact
		Params.EXACT_TOPK = true;
		topk_alg.executeQuery(user, query, k, 10000, newQuery, 100000);
		topk_alg.reinitialize(words, 1);
		Params.EXACT_TOPK = false;
		JsonObject jsonResult = topk_alg.getJsonExactTopK_vs_t(k);
		return jsonResult;
	}

	public JsonObject executeIncrementalVsNonincrementalQuery(String user, List<String> query, 
			int k, float alpha, int lengthPrefixMinimum) throws SQLException {
		// Computation for infinity (oracle)
		Params.EXACT_TOPK = false;
		String keyword = query.get(0);
		topk_alg.setAlpha(alpha);
		String[] words = new String[query.size()];
		int i = 0;

		List<String> currentQuery = new ArrayList<String>();
		// JSON results
		JsonObject jsonResult = new JsonObject();
		jsonResult.add("status", new JsonPrimitive(1));

		System.out.println("echo");
		// Compute the different oracles for each prefix length
		List<Set<Long>> oracles = new ArrayList<Set<Long>>(); // Exact top k for each prefix length
		for (int l=lengthPrefixMinimum; l<=keyword.length(); l++) {
			currentQuery = new ArrayList<String>();
			currentQuery.add(keyword.substring(0, l));
			topk_alg.executeQuery(user, currentQuery, k, 10000, true, 100000);
			topk_alg.computeTopkInfinity(k);
			oracles.add(topk_alg.getTopkInfinity());
			i = 0;
			for (String term: query) {
				words[i] = term;
				i++;
			}
			topk_alg.reinitialize(words, 1);
		}
		System.out.println("echo2");
		// TESTS here for incremental version
		JsonArray arrayResultsIncremental = new JsonArray();
		Params.EXACT_TOPK = true;
		int index = 0;
		for (int l=lengthPrefixMinimum; l<=keyword.length(); l++) {
			currentQuery = new ArrayList<String>();
			currentQuery.add(keyword.substring(0, l));
			topk_alg.setTopkInfinity(oracles.get(index));
			System.out.println("echo3");
			if (l==lengthPrefixMinimum) {
				currentQuery = new ArrayList<String>();
				currentQuery.add(keyword.substring(0, l));
				topk_alg.executeQuery(user, query, k, 10000, true, 100000);
			}
			else {
				topk_alg.executeQueryPlusLetter(user, query, l, 10000);
			}
			index++;

			// JSON
			JsonObject currItem = new JsonObject();
			currItem.add("l", new JsonPrimitive(l));					// time spent
			currItem.add("time", new JsonPrimitive(topk_alg.getTimeTopK()));
			arrayResultsIncremental.add(currItem);
		}
		topk_alg.reinitialize(words, 1);

		// TESTS for non incremental version
		JsonArray arrayResultsNonIncremental = new JsonArray();
		index = 0;
		for (int l=lengthPrefixMinimum; l<=keyword.length(); l++) {
			currentQuery = new ArrayList<String>();
			currentQuery.add(keyword.substring(0, l));
			topk_alg.setTopkInfinity(oracles.get(index));
			topk_alg.executeQuery(user, query, k, 10000, true, 100000);
			index++;
			
			// JSON
			JsonObject currItem = new JsonObject();
			currItem.add("l", new JsonPrimitive(l));					// time spent
			currItem.add("time", new JsonPrimitive(topk_alg.getTimeTopK()));
			arrayResultsNonIncremental.add(currItem);

			topk_alg.reinitialize(words, 1);
		}

		Params.EXACT_TOPK = false;
		jsonResult.add("incremental", arrayResultsIncremental);
		jsonResult.add("notincremental", arrayResultsNonIncremental);
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
