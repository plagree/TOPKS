package org.dbweb.Arcomem.Integration;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * Two methods:
 * - load data in memory from the network and triple files.
 * - run an HTTP Server waiting for GET requests at URL:
 * 		 localhost:PORT/topks?q=query&seeker=seeker&t=timeInMs&newQuery=boolean&nNeigh=nVisitedNeighbors&alpha=socialParam
 * 	 and returning JSON answers
 * 
 * @author Paul
 *
 */
@SuppressWarnings("restriction")
public class TOPKSServer {

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
			server.createContext("/topks", new TOPKSHandler());
			server.setExecutor(null); // creates a default executor
			server.start();
			System.out.println("Server started on port "+PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static class TOPKSHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			Map<String, String> params = TOPKSServer.queryToMap(t.getRequestURI().getQuery());
			Headers h = t.getResponseHeaders();
			h.add("Content-Type", "application/json; charset=UTF-8");
			StringBuilder responseBuffer = new StringBuilder(); // put the response text in this buffer to be sent out at the end
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
				/*for (String word : params.get("q").split("+")) //TODO multiple words
					query.add(word);*/
				query.add(params.get("q"));

				// Execute the TOPKS query with data parsed from the URI
				JsonObject jsonAnswer;
				try {
					jsonAnswer = TOPKSServer.topksSearcher.executeQuery(
							params.get("seeker"), 
							query, 
							Integer.parseInt(params.get("k")), 
							Integer.parseInt(params.get("t")),
							Boolean.parseBoolean(params.get("newQuery")),
							Integer.parseInt(params.get("nNeigh")),
							Float.parseFloat(params.get("alpha"))
							);

					//System.out.println(jsonAnswer.toString());
					
					// Create JSON response
					jsonResponse.add("n", jsonAnswer.get("n"));
					jsonResponse.add("status", jsonAnswer.get("status"));
					jsonResponse.add("nLoops", jsonAnswer.get("nLoops"));
					jsonResponse.add("results", jsonAnswer.get("results"));

				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (SQLException e) {
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
	}

	public static Map<String, String> queryToMap(String query){
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

}
