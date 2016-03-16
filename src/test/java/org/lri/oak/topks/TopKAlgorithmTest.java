package org.lri.oak.topks;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.dbweb.experiments.JsonBuilder;
import org.dbweb.socialsearch.shared.Params;
import org.dbweb.socialsearch.topktrust.algorithm.TopKAlgorithm;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathMultiplication;
import org.dbweb.socialsearch.topktrust.algorithm.paths.OptimalPaths;
import org.dbweb.socialsearch.topktrust.algorithm.score.Score;
import org.dbweb.socialsearch.topktrust.algorithm.score.TfIdfScore;

import com.google.gson.JsonObject;

public class TopKAlgorithmTest {

  public static void main(String[] args) {
    Params.dir = System.getProperty("user.dir") + "/test/yelp/TOPZIP/small/";
    Params.networkFile = "network.txt";
    Params.triplesFile = "triples.txt";
    // Index files and load data in memory
    Score score = new TfIdfScore();
    OptimalPaths optpath = new OptimalPaths("network", true);
    TopKAlgorithm algo = new TopKAlgorithm(score, 0f, new PathMultiplication(), optpath);
    List<String> query = new ArrayList<String>();
    query.add("restaurant");
    algo.executeQuery(1, query, 3, 0f, 2000, 200000);
    JsonObject jsonResult = JsonBuilder.getJsonAnswer(algo, 1000);
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
