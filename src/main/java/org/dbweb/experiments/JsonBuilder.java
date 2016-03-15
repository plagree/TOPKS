package org.dbweb.experiments;

import org.dbweb.socialsearch.topktrust.algorithm.TopKAlgorithm;
import org.dbweb.socialsearch.topktrust.datastructure.Item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class JsonBuilder {

  /**
   * Returns a JSON object containing different parameters
   * @param topk_alg TopKAlgorithm object which ran the experiment
   * @param k Number of items in top-k
   * @return JsonObject: JSON response
   */
  public static JsonObject getJsonAnswer(TopKAlgorithm alg, int k) {
    JsonObject jsonResult = new JsonObject();
    JsonArray arrayResults = new JsonArray();
    JsonObject currItem;
    int n = 0;
    for (Item item: alg.getCandidates().getListTopk(k)) {
      n++;
      if (item.getItemId() == 35397)
        item.debugging();
      currItem = new JsonObject();
      // id of the item
      currItem.add("id", new JsonPrimitive(item.getItemId()));
      // position of item
      currItem.add("rank", new JsonPrimitive(n));
      // completion of the term (term if already complete word)
      currItem.add("completion", new JsonPrimitive(item.getCompletion()));
      // sum( idf(t) * tf(item | t) for t in query)
      currItem.add("textualScore", new JsonPrimitive(item.getTextualScore()));
      // sum( idf(t) * sf(item | s, t) for t in query)
      currItem.add("socialScore", new JsonPrimitive(item.getSocialScore()));
      arrayResults.add(currItem);
    }
    // No problem appeared in TOPKS
    jsonResult.add("status", new JsonPrimitive(1));
    // Number of loops in TOPKS
    jsonResult.add("nLoops", new JsonPrimitive(alg.getNumloops()));
    // Number of Disk accesses for p-spaces
    jsonResult.add("nbPSpacesAccesses",
                   new JsonPrimitive(alg.getPSpaceAccesses()));
    // Number of results
    jsonResult.add("n", new JsonPrimitive(n));
    // Array of the results
    jsonResult.add("results", arrayResults);
    //System.out.println(jsonResult.toString());
    return jsonResult;
  }

}