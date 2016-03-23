package org.dbweb.Arcomem.Integration;

import org.dbweb.socialsearch.shared.Params;
import org.dbweb.socialsearch.topktrust.algorithm.score.Score;
import org.dbweb.socialsearch.topktrust.algorithm.score.TfIdfScore;

public class Main {

	public static void main(String[] args) {
		if (args.length != 4) {
			System.out.println("Usage: java -jar -Xmx13000m executable.jar"
			        + "/path/to/files.txt networkFile triplesFile thresholdRef"
			        + "\nYou gave "+args.length+" parameters");
			for (int i=0; i<args.length; i++) {
				System.out.println("Argument " + (i + 1) + ": " + args[i]);
			}
			System.exit(0);
		}
		
		Params.dir = args[0];
		//Params.number_documents = Integer.parseInt(args[1]);
		Params.networkFile = args[1];
		Params.triplesFile = args[2];
		Params.threshold = Float.parseFloat(args[3]);
		
		// Index files and load data in memory
		Score score = new TfIdfScore(); // Tfidf scoring
		TOPKSSearcher topksSearcher = new TOPKSSearcher(score);
		topksSearcher.setSkippedTests(500);
		// Start a server listening to queries
		TOPKSServer topksServer = new TOPKSServer(topksSearcher);
		topksServer.run();
	}

}
