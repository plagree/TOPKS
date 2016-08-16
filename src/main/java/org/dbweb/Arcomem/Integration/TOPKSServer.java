package org.dbweb.Arcomem.Integration;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dbweb.socialsearch.shared.Params;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * 
 * Three methods:
 * - load data in memory from the network and triple files.
 * - run an HTTP Server waiting for GET requests at URL:
 * 		 localhost:PORT/topks?q=query&seeker=seeker&t=timeInMs&newQuery=boolean&nNeigh=nVisitedNeighbors&alpha=socialParam
 * 	 and returning JSON answers
 * - run an HTTP Server waiting for GET requests at URL:
 * 		 localhost:PORT/asyt?q=query&seeker=seeker&t=timeInMs&nNeigh=nVisitedNeighbors&alpha=socialParam&l=lengthPrefixMin
 * 	 and returning JSON answers
 * 
 * @author Paul
 *
 */
@SuppressWarnings("restriction")
public class TOPKSServer {

  private static enum Query {TOPKS, INCREMENTAL, BASELINE}
  private final static int PORT = 8000;
  public static TOPKSSearcher topksSearcher;

  public TOPKSServer(TOPKSSearcher topksSearcher) {
    TOPKSServer.topksSearcher = topksSearcher;
  }

  public void run() {
    // Start server
    HttpServer server;
    try {
      server = HttpServer.create(new InetSocketAddress(PORT), 0);
      // TOPKS single query
      server.createContext("/topks", new TOPKSHandler());
      //server.createContext("/asyt", new ASYTHandler());			// TOPKS ASYT Incremental
      //server.createContext("/ndcg", new NDCGHandler());			// TOPKS NDCG vs time experiment
      //server.createContext("/exact_topk", new ExactTopkHandler());// TOPKS time exact topk vs l prefix experiment
      //server.createContext("/social_baseline", new SocialBaselineHandler()); // TOPKS with social baseline (union of ILs of completions)
      //server.createContext("/supernodes", new SuperNodesHandler()); // TOPKS with super nodes (clusters)
      //server.createContext("/incremental", new ExactTopkIncVsNonincHandler()); // TOPKS time for exact topk incremental vs not
      //server.setExecutor(null); 									// creates a default executor
      /*
       * Different baselines:
       * ====================
       *    * topks_autocomplete: queries are autocompleted with Yelp API then normal TOPKS
       *        - `query` = "my+prefix"
       *        - `autocompletions` = "first+autocompletion_second+autocompletion"
       */
      server.createContext("/baselines", new BaselineHandler());
      server.start();
      System.out.println("Server started on port " + PORT);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * This Class handles TOPKS requests. Last word is considered as prefix,
   * others as complete words. The answer is an OR semantic in multiple word
   * queries.
   * 
   * query: /topks?q=term1+term2+pref&seeker=1&alpha=0&k=20&t=100&nNeigh=2000
   * 
   * @author Paul Lagrée
   *
   */
  private static class TOPKSHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      TOPKSServer.handleQuery(t, Query.TOPKS);
    }
  }

  /**
   * query: /ndcg?...
   * @author lagree
   *
   */
  /*private static class NDCGHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      Map<String, String> params = TOPKSServer.queryToMap(t.getRequestURI().getQuery());
      Headers h = t.getResponseHeaders();
      h.add("Content-Type", "application/json; charset=UTF-8");
      // put the response text in this buffer to be sent out at the end
      StringBuilder responseBuffer = new StringBuilder();
      String response;
      JsonObject jsonResponse = new JsonObject();
      Gson gson = new GsonBuilder().setPrettyPrinting().create();

      // If we didn't receive a request in the right format
      if (!params.containsKey("q") && !params.containsKey("seeker")) {
        jsonResponse.add("status", new JsonPrimitive(0));
        responseBuffer.append(jsonResponse.toString());
        response = responseBuffer.toString();
        t.sendResponseHeaders(400, response.length());
      }
      else {
        System.out.println(t.getRequestURI());

        // Create the query List of words
        List<String> query = new ArrayList<String>();
        //for (String word : params.get("q").split("+")) //TODO multiple words
				//	query.add(word);
        query.add(params.get("q"));

        try {
          if (params.get("mode").equals("time")) {
            jsonResponse = TOPKSServer.topksSearcher.executeQueryNDCG_vs_time(
                Integer.parseInt(params.get("seeker")), 
                query, 
                Integer.parseInt(params.get("k")), 
                Integer.parseInt(params.get("t")),
                Boolean.parseBoolean(params.get("newQuery")),
                Integer.parseInt(params.get("nNeigh")),
                Float.parseFloat(params.get("alpha"))
                );
          }
          else if (params.get("mode").equals("users")) {
            jsonResponse = TOPKSServer.topksSearcher.executeQueryNDCG_vs_nbusers(
                params.get("seeker"), 
                query, 
                Integer.parseInt(params.get("k")), 
                Integer.parseInt(params.get("t")),
                Boolean.parseBoolean(params.get("newQuery")),
                Integer.parseInt(params.get("nNeigh")),
                Float.parseFloat(params.get("alpha"))
                );
          }

        } catch (NumberFormatException e) {
          e.printStackTrace();
        }
        responseBuffer.append(gson.toJson(jsonResponse).toString());
        response = responseBuffer.toString();
        t.sendResponseHeaders(200, response.length());
      }

      OutputStream os = t.getResponseBody();
      os.write(response.getBytes("UTF-8"));
      os.flush();
      os.close();
      t.close();
    }
  }*/

  /**
   * query: /incremental?...
   * @author lagree
   *
   */
  /*private static class ExactTopkIncVsNonincHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      Map<String, String> params = TOPKSServer.queryToMap(
          t.getRequestURI().getQuery());
      Headers h = t.getResponseHeaders();
      h.add("Content-Type", "application/json; charset=UTF-8");
      // put the response text in this buffer to be sent out at the end
      StringBuilder responseBuffer = new StringBuilder();
      String response;
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      JsonObject jsonResponse = new JsonObject();

      // If we didn't receive a request in the right format
      if (!params.containsKey("q") && !params.containsKey("seeker")) {
        jsonResponse.add("status", new JsonPrimitive(0));
        responseBuffer.append(jsonResponse.toString());
        response = responseBuffer.toString();
        t.sendResponseHeaders(400, response.length());
      }
      else {
        System.out.println(t.getRequestURI());

        // Create the query List of words
        List<String> query = new ArrayList<String>();
        query.add(params.get("q"));

        try {
          jsonResponse = TOPKSServer.topksSearcher.executeIncrementalVsNonincrementalQuery(
              params.get("seeker"), 
              query, 
              Integer.parseInt(params.get("k")),
              Float.parseFloat(params.get("alpha")),
              3
              );
        } catch (NumberFormatException e) {
          e.printStackTrace();
        }
        responseBuffer.append(gson.toJson(jsonResponse).toString());
        response = responseBuffer.toString();
        t.sendResponseHeaders(200, response.length());
      }

      OutputStream os = t.getResponseBody();
      os.write(response.getBytes("UTF-8"));
      os.flush();
      os.close();
      t.close();
    }
  }*/

  /**
   * query: /exact_topk?...
   * @author lagree
   *
   */
  /*private static class ExactTopkHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      Map<String, String> params = TOPKSServer.queryToMap(
          t.getRequestURI().getQuery());
      Headers h = t.getResponseHeaders();
      h.add("Content-Type", "application/json; charset=UTF-8");
      // put the response text in this buffer to be sent out at the end
      StringBuilder responseBuffer = new StringBuilder();
      String response;
      JsonObject jsonResponse = new JsonObject();
      Gson gson = new GsonBuilder().setPrettyPrinting().create();

      // If we didn't receive a request in the right format
      if (!params.containsKey("q") && !params.containsKey("seeker")) {
        jsonResponse.add("status", new JsonPrimitive(0));
        responseBuffer.append(jsonResponse.toString());
        response = responseBuffer.toString();
        t.sendResponseHeaders(400, response.length());
      }
      else {
        System.out.println(t.getRequestURI());

        // Create the query List of words
        List<String> query = new ArrayList<String>();
        //for (String word : params.get("q").split("+")) //TODO multiple words
				//	query.add(word);
        query.add(params.get("q"));

        try {
          jsonResponse = TOPKSServer.topksSearcher.executeQueryExactTopK_vs_time(
              params.get("seeker"), 
              query, 
              Integer.parseInt(params.get("k")),
              Boolean.parseBoolean(params.get("newQuery")),
              Float.parseFloat(params.get("alpha"))
              );
        } catch (NumberFormatException e) {
          e.printStackTrace();
        }
        responseBuffer.append(gson.toJson(jsonResponse).toString());
        response = responseBuffer.toString();
        t.sendResponseHeaders(200, response.length());
      }

      OutputStream os = t.getResponseBody();
      os.write(response.getBytes("UTF-8"));
      os.flush();
      os.close();
      t.close();
    }
  }*/

  /**
   * query: /asyt?...
   * @author lagree
   *
   */
  /*private static class ASYTHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      Map<String, String> params = TOPKSServer.queryToMap(t.getRequestURI().getQuery());
      Headers h = t.getResponseHeaders();
      h.add("Content-Type", "application/json; charset=UTF-8");
      // put the response text in this buffer to be sent out at the end
      StringBuilder responseBuffer = new StringBuilder();
      String response;
      JsonObject jsonResponse = new JsonObject();
      Gson gson = new GsonBuilder().setPrettyPrinting().create();

      // If we didn't receive a request in the right format
      if (!params.containsKey("q") && !params.containsKey("seeker")) {
        jsonResponse.add("status", new JsonPrimitive(0));
        responseBuffer.append(jsonResponse.toString());
        response = responseBuffer.toString();
        t.sendResponseHeaders(400, response.length());
      }
      else {
        System.out.println(t.getRequestURI());

        jsonResponse.add("status", new JsonPrimitive(1));

        // Create the query List of words
        List<String> query = new ArrayList<String>();
        //for (String word : params.get("q").split("+")) //TODO multiple words
				//	query.add(word);
        query.add(params.get("q"));

        try {
          jsonResponse = TOPKSServer.topksSearcher.executeIncrementalQuery(
              params.get("seeker"), 
              query, 
              Integer.parseInt(params.get("k")), 
              Integer.parseInt(params.get("t")),
              Integer.parseInt(params.get("nNeigh")),
              Float.parseFloat(params.get("alpha")),
              Integer.parseInt(params.get("l"))
              );
          if (Params.VERBOSE)
            System.out.println(jsonResponse.toString());
        } catch (NumberFormatException e) {
          e.printStackTrace();
        }
        responseBuffer.append(gson.toJson(jsonResponse).toString());
        response = responseBuffer.toString();
        t.sendResponseHeaders(200, response.length());
      }

      OutputStream os = t.getResponseBody();
      os.write(response.getBytes("UTF-8"));
      os.flush();
      os.close();
      t.close();
    }
  }*/

  /**
   * query: /baselines?q=term1+term2+pref&seeker=1&alpha=0&k=20&disk_budget=50
   * @author Paul Lagrée
   *
   */
  private static class BaselineHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      TOPKSServer.handleQuery(t, Query.BASELINE);
    }
  }

  /**
   * 
   * query: /supernodes?...
   * @author lagree
   *
   */
  /*private static class SuperNodesHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      Map<String, String> params = TOPKSServer.queryToMap(
          t.getRequestURI().getQuery());
      Headers h = t.getResponseHeaders();
      h.add("Content-Type", "application/json; charset=UTF-8");
      // put the response text in this buffer to be sent out at the end
      StringBuilder responseBuffer = new StringBuilder();
      String response;
      JsonObject jsonResponse = new JsonObject();
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      // If we didn't receive a request in the right format
      if (!params.containsKey("q") && !params.containsKey("seeker")) {
        jsonResponse.add("status", new JsonPrimitive(0));
        responseBuffer.append(jsonResponse.toString());
        response = responseBuffer.toString();
        t.sendResponseHeaders(400, response.length());
      }
      else {
        System.out.println(t.getRequestURI());

        // Create the query List of words
        List<String> query = new ArrayList<String>();
        query.add(params.get("q"));
        try {
          jsonResponse = TOPKSServer.topksSearcher.executeSupernodes(
              params.get("seeker"),
              query,
              Integer.parseInt(params.get("k")),
              true,
              Float.parseFloat(params.get("alpha")),
              Integer.parseInt(params.get("disk_budget")), // Number of accesses allowed to the disk (budget)
              Boolean.parseBoolean(params.get("supernode"))	 // true if TOPKS-ASYT (false if supernode mode)
              );
        } catch (NumberFormatException e) {
          e.printStackTrace();
        }
        responseBuffer.append(gson.toJson(jsonResponse).toString());
        response = responseBuffer.toString();
        t.sendResponseHeaders(200, response.length());
      }

      OutputStream os = t.getResponseBody();
      os.write(response.getBytes("UTF-8"));
      os.flush();
      os.close();
      t.close();
    }
  }*/

  public static Map<String, String> queryToMap(String query) {
    Map<String, String> result = new HashMap<String, String>();
    if (query == null)
      return result;
    for (String param : query.split("&")) {
      String pair[] = param.split("=");
      if (pair.length>1) {
        result.put(pair[0], pair[1]);
      }else{
        result.put(pair[0], "");
      }
    }
    return result;
  }

  /**
   * Run the query, this method is common to all types of queries.
   * @param type Type of the query to handle (among the enumeration types)
   * @return <code>true</code> if the query was correct, <code>false</code>
   *         otherwise
   * @throws IOException
   */
  private static void handleQuery(HttpExchange t, Query type) throws IOException {
    Map<String, String> params = TOPKSServer.queryToMap(
            t.getRequestURI().getQuery());
    JsonObject jsonResponse = new JsonObject();
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    Headers h = t.getResponseHeaders();
    h.add("Content-Type", "application/json; charset=UTF-8");
    // put the response text in this buffer to be sent out at the end
    StringBuilder responseBuffer = new StringBuilder();
    String response;
    System.out.println(t.getRequestURI());
    try {
      switch (type) {
        case BASELINE: jsonResponse = TOPKSServer.runBaseline(params);
          break;
        case TOPKS: jsonResponse = TOPKSServer.runTOPKS(params);
          break;
        default: jsonResponse = null;
      }
    } catch (NumberFormatException e) {
      e.printStackTrace();
    }
    if (jsonResponse != null) {
      responseBuffer.append(gson.toJson(jsonResponse).toString());
      response = responseBuffer.toString();
      t.sendResponseHeaders(200, response.length());
    } else { // Query not in right format
      jsonResponse = new JsonObject();
      jsonResponse.add("status", new JsonPrimitive(0));
      responseBuffer.append(jsonResponse.toString());
      response = responseBuffer.toString();
      t.sendResponseHeaders(400, response.length());
    }
    OutputStream os = t.getResponseBody();
    os.write(response.getBytes("UTF-8"));
    os.flush();
    os.close();
    t.close();
  }

  /**
   * Method to call classic TOPKS-ASYT Algorithm.
   * @param params
   * @return
   */
  public static JsonObject runTOPKS(Map<String, String> params) {
    JsonObject jsonResponse;
    if (params.containsKey("q") && params.containsKey("seeker") &&
            params.containsKey("alpha") && params.containsKey("t") &&
            params.containsKey("nNeigh")) {
      // Create the query List of words
      List<String> query = Arrays.asList(params.get("q").split("\\+"));
      jsonResponse = TOPKSServer.topksSearcher.executeQuery(
              Integer.parseInt(params.get("seeker")), query, 
              Integer.parseInt(params.get("k")), 
              Integer.parseInt(params.get("t")),
              Integer.parseInt(params.get("nNeigh")),
              Float.parseFloat(params.get("alpha")),
              Boolean.parseBoolean(params.get("multiWordsQuery"))
              );
    } else {
      jsonResponse = null;
    }
    return jsonResponse;
  }

  /**
   * Method to call compare TOPKS-ASYT to different baselines.
   * @param params
   * @return
   */
  public static JsonObject runBaseline(Map<String, String> params) {
    JsonObject jsonResponse;
    if (params.containsKey("q") && params.containsKey("seeker") &&
            params.containsKey("alpha") && params.containsKey("disk_budget") &&
            params.containsKey("base")) {
      // Create the query List of words
      List<String> query = Arrays.asList(params.get("q").split("\\+"));
      if (params.get("base").equals("topks_m")) {   // TOPKS_M
        Params.CHOSEN_BASELINE = Baseline.TOPKS_M;
        jsonResponse = TOPKSServer.topksSearcher.executeBaseline(
                Integer.parseInt(params.get("seeker")), query,
                Integer.parseInt(params.get("k")),
                Float.parseFloat(params.get("alpha")),
                Integer.parseInt(params.get("disk_budget")),
                Baseline.TOPKS_M);
      } else if (params.get("base").equals("textual_social")) { // TOPKS_2D
        Params.CHOSEN_BASELINE = Baseline.TEXTUAL_SOCIAL;
        jsonResponse = TOPKSServer.topksSearcher.executeBaseline(
              Integer.parseInt(params.get("seeker")), query,
              Integer.parseInt(params.get("k")),
              Float.parseFloat(params.get("alpha")),
              Integer.parseInt(params.get("disk_budget")),
              Baseline.TEXTUAL_SOCIAL);
      } else if (params.get("base").equals("topk_merge")) { // TOPK_MERGE
        Params.CHOSEN_BASELINE = Baseline.TOPK_MERGE;
        jsonResponse = TOPKSServer.topksSearcher.executeBaseline(
                Integer.parseInt(params.get("seeker")), query,
                Integer.parseInt(params.get("k")),
                Float.parseFloat(params.get("alpha")),
                Integer.parseInt(params.get("disk_budget")),
                Baseline.TOPK_MERGE);
      } else if (params.get("base").equals("topks_autocomplete") &&
              params.containsKey("autocompletions")) {  // AUTOCOMPLETION
        Params.CHOSEN_BASELINE = Baseline.AUTOCOMPLETION;
        List<List<String>> query_autocompletions = new ArrayList<List<String>>();
        List<String> string_queries = Arrays.asList(params.get("autocompletions").split("_"));
        for (String string_query: string_queries) {
          query_autocompletions.add(Arrays.asList(string_query.split("\\+")));
        }
        jsonResponse = TOPKSServer.topksSearcher.executeBaselineAutocompletions(
                Integer.parseInt(params.get("seeker")), query,
                Integer.parseInt(params.get("k")),
                Float.parseFloat(params.get("alpha")),
                Integer.parseInt(params.get("disk_budget")),
                query_autocompletions);
      } else
        jsonResponse = null;
    } else {
      jsonResponse = null;
    }
    Params.CHOSEN_BASELINE = null;
    return jsonResponse;
  }

}