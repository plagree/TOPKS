package org.dbweb.Arcomem.Integration;

import java.util.List;

import org.dbweb.experiments.JsonBuilder;
import org.dbweb.socialsearch.shared.Params;
import org.dbweb.socialsearch.topktrust.algorithm.TopKAlgorithm;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathCompositionFunction;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathMultiplication;
import org.dbweb.socialsearch.topktrust.algorithm.paths.OptimalPaths;
import org.dbweb.socialsearch.topktrust.algorithm.score.Score;
import org.dbweb.socialsearch.topktrust.datastructure.Item;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class TOPKSSearcher {

  private static final boolean heap = true;
  private static final PathCompositionFunction<Float> pathFunction =
          new PathMultiplication();
  public static final String network = "network";
  private TopKAlgorithm topk_alg;

  public TOPKSSearcher(Score score) {
    OptimalPaths optpath = new OptimalPaths(network, heap);
    this.topk_alg = new TopKAlgorithm(score, 0f, pathFunction, optpath);
  }

  /**
   * Execute a normal query
   * @param k number maximum of returned results
   * @param t time given to compute an answer
   * @param user id of the seeker
   * @param nNeigh 
   * @return JsonObject containing the lower and upper score for every returned item
   */
  public JsonObject executeQuery(int seeker, List<String> query, int k,
          int t, int nNeigh, float alpha) {
    this.topk_alg.setSkippedTests(50);
    this.topk_alg.executeQuery(seeker, query, k, alpha, t, nNeigh,
            Experiment.DEFAULT);
    this.topk_alg.reset(query, 1);
    float fixing[][] = {//{200, 100, 50, 40, 20, 10, 5, 2, 1, 1}, // alpha == 0
                        {64, 20, 5, 2, 1, 2, 1, 1, 1, 1},
                        //{100, 50, 25, 13, 16, 3, 2, 1, 1, 1}, // alpha == 0.01
                        {64, 20, 5, 2, 1, 2, 1, 1, 1, 1},
                        //{64, 40, 18, 10, 5, 2, 1, 1, 1, 1}, // alpha == 0.1
                        {64, 20, 5, 2, 1, 2, 1, 1, 1, 1},
                        {160, 60, 30, 10, 5, 2, 1, 1, 1, 1}};// alpha = 1.
    if (query.size() > 1) {
      if (alpha < 0.005)
          this.topk_alg.reorder(fixing[0][query.get(query.size()-1).length() - 1]);
      else if (alpha < 0.05)
          this.topk_alg.reorder(fixing[1][query.get(query.size()-1).length() - 1]);
      else if (alpha < 0.5)
          this.topk_alg.reorder(fixing[2][query.get(query.size()-1).length() - 1]);
      else if (alpha > 0.9)
          this.topk_alg.reorder(fixing[3][query.get(query.size()-1).length() - 1]);
      else
        System.out.println("Error: forbidden correction");
    }
    JsonObject jsonResult = JsonBuilder.getJsonAnswer(query, this.topk_alg, k);
    return jsonResult;
  }

  /*public JsonObject executeQueryNDCG_vs_time(String user, List<String> query,
	        int k, int t, boolean newQuery, int nNeigh, float alpha) {
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

	public JsonObject executeQueryNDCG_vs_nbusers(String user, List<String> query,
	        int k, int t, boolean newQuery, int nNeigh, float alpha) {
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

	public JsonObject executeQueryExactTopK_vs_time(String user,
	        List<String> query, int k, boolean newQuery, float alpha) {
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
	}*/

  public JsonObject executeBaseline(int seeker, List<String> query,
          int k, float alpha, int budget, Baseline baseline) {

    Params.DISK_BUDGET = budget;
    // Oracle computation (visit of whole graph)
    this.topk_alg.setSkippedTests(100000);
    this.topk_alg.executeQuery(seeker, query, k, alpha, 10000, 100000,
            Experiment.DEFAULT);
    this.topk_alg.computeOracleNDCG(k);
    this.topk_alg.reset(query, 1);

    // Computation for top-k exact: normal version
    this.setSkippedTests(1);

    if (baseline == Baseline.TEXTUAL_SOCIAL) {
      this.topk_alg.executeQuery(seeker, query, k, alpha, 30000, 100000,
              Experiment.NDCG_DISK_ACCESS);
    } else if (baseline == Baseline.TOPK_MERGE) {
      this.topk_alg.executeQuery(seeker, query, k, alpha, 30000, 100000,
              Experiment.DEFAULT);
    } else {
      throw new UnsupportedOperationException();
    }
    this.topk_alg.reset(query, 1);

    JsonObject obj_topk_asyt = new JsonObject();
    obj_topk_asyt.add("users_visited", new JsonPrimitive(
            this.topk_alg.getNumberUsersSeen()));
    obj_topk_asyt.add("inverted_lists_algo", new JsonPrimitive(
            this.topk_alg.getNumberInvertedListUsed()));
    obj_topk_asyt.add("ndcg", new JsonPrimitive(this.topk_alg.computeNDCG(k)));

    // Computation for top-k exact: baseline
    float ndcg = this.topk_alg.executeJournalBaselineQuery(seeker, query, k, alpha,
            30000, 100000, baseline);
    this.topk_alg.reset(query, 1);
    JsonObject obj_baseline = new JsonObject();
    obj_baseline.add("users_visited", new JsonPrimitive(
            this.topk_alg.getNumberUsersSeen()));
    obj_baseline.add("inverted_lists_algo", new JsonPrimitive(
            this.topk_alg.getNumberInvertedListUsed()));
    obj_baseline.add("ndcg", new JsonPrimitive(ndcg));

    // Create JSON Response
    JsonObject jsonResult = new JsonObject();
    jsonResult.add("status", new JsonPrimitive(1));
    jsonResult.add("topks_asyt", obj_topk_asyt);
    jsonResult.add("baseline", obj_baseline);

    return jsonResult;
  }

  /*public JsonObject executeSupernodes(String user, List<String> query, int k,
	        boolean newQuery, float alpha, int budget, boolean supernode) {
		Params.EXACT_TOPK = false;
		Params.DISK_ACCESS_EXPERIMENT = false;
		Params.DISK_BUDGET = budget;
		Params.SUPERNODE = supernode;
		topk_alg.setAlpha(alpha);
		String oracle = "";

		String[] words = new String[query.size()];
		int i = 0;
		for (String term: query) {
			words[i] = term;
			i++;
		}

		if (!Params.SUPERNODE) {
			// Oracle computation (visit of whole graph)
			topk_alg.executeQuery(user, query, k, 10000, newQuery, 100000);
			topk_alg.computeOracleNDCG(k);
			this.setSkippedTests(500); // No need to recompute everything too often
			oracle = topk_alg.getListTopK(k);
			topk_alg.reinitialize(words, 1);
		}

		Params.DISK_ACCESS_EXPERIMENT = true;
		this.setSkippedTests(1);
		topk_alg.executeQuery(user, query, k, 30000, newQuery, 100000);
		String result = topk_alg.getListTopK(k);
		topk_alg.reinitialize(words, 1);
		this.setSkippedTests(500);
		Params.DISK_ACCESS_EXPERIMENT = false;
		Params.SUPERNODE = false;

		// Create JSON Response
		JsonObject jsonResult = new JsonObject();
		// No problem appeared in TOPKS
		jsonResult.add("status", new JsonPrimitive(1));
		jsonResult.add("oracle", new JsonPrimitive(oracle));
		jsonResult.add("result", new JsonPrimitive(result));

		return jsonResult;
	}*/

  /*public JsonObject executeMixedBaseline(int seeker, List<String> query,
	        int k, float alpha) {
		Params.EXACT_TOPK = false;
		// Oracle computation (visit of whole graph)
		this.topk_alg.executeQuery(seeker, query, k, alpha, 10000, 100000);
		this.topk_alg.computeTopkInfinity(k);
		List<Long> oracleNDCG = this.topk_alg.getCandidates().getLongListTopk(k);
		this.topk_alg.reset(query, 1);

		// Computation TOPKS-ASYT
		Params.EXACT_TOPK = true;
		topk_alg.executeQuery(seeker, query, k, alpha, 10000, 100000);
		double ndcg_topks_asyt = NDCG.getNDCG(oracleNDCG,
		    this.topk_alg.getCandidates().getLongListTopk(k), k);
		topk_alg.reset(query, 1);
		long time_topks_asyt = topk_alg.getTimeTopK();
		Params.EXACT_TOPK = false;

		// NDCG for mixed version
		long time_baseline = 0;

		// Create JSON Response
		JsonObject jsonResult = new JsonObject();
		jsonResult.add("status", new JsonPrimitive(1));
		JsonObject obj = new JsonObject();
		jsonResult.add("topks_asyt", new JsonPrimitive(time_topks_asyt));
		jsonResult.add("baseline", new JsonPrimitive(time_baseline));
		return jsonResult;
	}*/

  /*public JsonObject executeIncrementalVsNonincrementalQuery(String user,
	        List<String> query, int k, float alpha, int lengthPrefixMinimum) {
		// Computation for infinity (oracle)
		Params.EXACT_TOPK = false;
		String keyword = query.get(0);
		topk_alg.setAlpha(alpha);
		String[] words = new String[query.size()];
		int i = 0;
		for (String term: query) {
			words[i] = term;
			i++;
		}

		List<String> currentQuery = new ArrayList<String>();
		// JSON results
		JsonObject jsonResult = new JsonObject();
		jsonResult.add("status", new JsonPrimitive(1));

		System.out.println("ORACLE COMPUTATION\n=====");
		// Compute the different oracles for each prefix length
		List<Set<Long>> oracles = new ArrayList<Set<Long>>(); // Exact top k for each prefix length
		for (int l=lengthPrefixMinimum; l<=keyword.length(); l++) {
			currentQuery = new ArrayList<String>();
			currentQuery.add(keyword.substring(0, l));
			topk_alg.executeQuery(user, currentQuery, k, 10000, true, 100000);
			topk_alg.computeTopkInfinity(k);
			oracles.add(topk_alg.getTopkInfinity());
			topk_alg.reinitialize(words, 1);
		}
		System.out.println("INCREMENTAL TESTS\n=====");
		// TESTS here for incremental version
		JsonArray arrayResultsIncremental = new JsonArray();
		Params.EXACT_TOPK = true;
		int index = 0;
		for (int l=lengthPrefixMinimum; l<=keyword.length(); l++) {
			currentQuery = new ArrayList<String>();
			currentQuery.add(keyword.substring(0, l));
			topk_alg.setTopkInfinity(oracles.get(index));
			if (l==lengthPrefixMinimum) {
				topk_alg.executeQuery(user, currentQuery, k, 10000, true, 100000);
			}
			else {
				topk_alg.executeQueryNextLetter(user, currentQuery, k, 100000);
			}
			index++;

			// JSON
			JsonObject currItem = new JsonObject();
			currItem.add("l", new JsonPrimitive(l));					// time spent
			currItem.add("q", new JsonPrimitive(keyword.substring(0, l)));
			currItem.add("time", new JsonPrimitive(topk_alg.getTimeTopK()));
			arrayResultsIncremental.add(currItem);
		}
		System.out.println("NON INCREMENTAL");
		topk_alg.reinitialize(words, 1);

		// TESTS for non incremental version
		JsonArray arrayResultsNonIncremental = new JsonArray();
		index = 0;
		for (int l=lengthPrefixMinimum; l<=keyword.length(); l++) {
			currentQuery = new ArrayList<String>();
			currentQuery.add(keyword.substring(0, l));
			topk_alg.setTopkInfinity(oracles.get(index));
			topk_alg.executeQuery(user, currentQuery, k, 10000, true, 100000);
			index++;

			// JSON
			JsonObject currItem = new JsonObject();
			currItem.add("l", new JsonPrimitive(l));					// time spent
			currItem.add("q", new JsonPrimitive(keyword.substring(0, l)));
			currItem.add("time", new JsonPrimitive(topk_alg.getTimeTopK()));
			arrayResultsNonIncremental.add(currItem);

			topk_alg.reinitialize(words, 1);
		}

		Params.EXACT_TOPK = false;
		jsonResult.add("incremental", arrayResultsIncremental);
		jsonResult.add("notincremental", arrayResultsNonIncremental);
		return jsonResult;
	}

	public JsonObject executeIncrementalQuery(String user, List<String> query,
	        int k, int t, int nNeigh, float alpha, int lengthPrefixMin) {
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
					topk_alg.executeQueryNextLetter(user, currQuery, l, t);
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
	}*/

  public void setSkippedTests(int skippedTests) {
    this.topk_alg.setSkippedTests(skippedTests);
  }

}