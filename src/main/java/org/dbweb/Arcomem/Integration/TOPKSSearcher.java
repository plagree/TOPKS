package org.dbweb.Arcomem.Integration;

import java.sql.SQLException;
import java.util.List;

import org.dbweb.socialsearch.topktrust.algorithm.TopKAlgorithm;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathCompositionFunction;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathMultiplication;
import org.dbweb.socialsearch.topktrust.algorithm.paths.OptimalPaths;
import org.dbweb.socialsearch.topktrust.algorithm.score.TfIdfScore;

import com.google.gson.JsonObject;

public class TOPKSSearcher {
	
	private static final boolean heap = true;
	@SuppressWarnings("rawtypes")
	private static final PathCompositionFunction pathFunction = new PathMultiplication();
	private static double coeff = 2.0f;
	public static final String network = "soc_snet_dt";
	public static final String taggers = "soc_tag_80";
	private static final int method = 1;
	private TopKAlgorithm topk_alg;

	public TOPKSSearcher() {
		
		OptimalPaths optpath;
		TfIdfScore score = new TfIdfScore();
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
		topk_alg.reinitialize(words,1);
		JsonObject jsonResult = topk_alg.getJsonAnswer(k);
		return jsonResult;
	}

	public void setSkippedTests(int skippedTests) {
		this.topk_alg.setSkippedTests(skippedTests);
	}
}
