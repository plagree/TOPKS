package org.lri.oak.topks;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.dbweb.Arcomem.Integration.Baseline;
import org.dbweb.Arcomem.Integration.Experiment;
import org.dbweb.experiments.JsonBuilder;
import org.dbweb.socialsearch.shared.Params;
import org.dbweb.socialsearch.topktrust.algorithm.TopKAlgorithm;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathMultiplication;
import org.dbweb.socialsearch.topktrust.algorithm.paths.OptimalPaths;
import org.dbweb.socialsearch.topktrust.algorithm.score.Score;
import org.dbweb.socialsearch.topktrust.algorithm.score.TfIdfScore;
import org.dbweb.socialsearch.topktrust.datastructure.Item;
import org.externals.Tools.ItemBaseline;
import org.externals.Tools.NDCG;

import com.google.gson.JsonObject;

public class Play {

  public static void main(String[] args) {
    /*List<Long> l1 = Arrays.asList(new Long[] {53356l, 43678l, 51834l, 43525l,
        42378l, 32215l, 32061l, 35348l, 42225l, 50040l, 44460l, 30661l, 47531l,
        37929l, 876l, 44786l, 30661l, 47781l, 42438l, 36783l});
    List<Long> l2 = Arrays.asList(new Long[] {53356l, 43678l, 51834l, 43525l,
        42378l, 32215l, 32061l, 35348l, 42225l, 50040l, 44460l, 30661l, 47531l,
        37929l, 876l, 44786l, 47781l, 42438l, 36783l, 12323l});*/
    List<Long> l1 = Arrays.asList(new Long[] {53356l, 43678l});
    List<Long> l2 = Arrays.asList(new Long[] {53356l, 43678l});
    System.out.println(l1 == l2);
    System.exit(1);
    Params.dir = System.getProperty("user.dir") + "/test/yelp/TOPZIP/small/";
    Params.networkFile = "network.txt";
    Params.triplesFile = "triples.txt";
    // Index files and load data in memory
    Score score = new TfIdfScore();
    OptimalPaths optpath = new OptimalPaths("network", true);
    TopKAlgorithm algo = new TopKAlgorithm(score, 0f, new PathMultiplication(), optpath);
    List<String> query = new ArrayList<String>();
    query.add("restaurant");
    //algo.executeQuery(1, query, 3, 0f, 2000, 200000, Experiment.DEFAULT);
    algo.executeJournalBaselineQuery(1, query, 3, 1, 2000, 200000, Baseline.TOPK_MERGE);
    JsonObject jsonResult = JsonBuilder.getJsonAnswer(query, algo, 1000);
    try (PrintWriter out = new PrintWriter("/home/paul/output.json", "UTF-8")){
      out.print(jsonResult.toString());
      out.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (UnsupportedEncodingException e1) {
      e1.printStackTrace();
    }
  }

}
