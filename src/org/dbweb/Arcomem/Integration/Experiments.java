package org.dbweb.Arcomem.Integration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;

import org.dbweb.socialsearch.shared.Params;
import org.dbweb.socialsearch.topktrust.algorithm.TopKAlgorithm;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathCompositionFunction;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathMultiplication;
import org.dbweb.socialsearch.topktrust.algorithm.paths.OptimalPaths;
import org.dbweb.socialsearch.topktrust.algorithm.score.BM25Score;
import org.dbweb.socialsearch.topktrust.datastructure.UserEntry;

public class Experiments {

	//private static float alpha = 0.0f;
	private static final boolean heap = true;
	private static final PathCompositionFunction pathFunction = new PathMultiplication();
	public static final String network = "soc_snet_dt";
	public static final String taggers = "soc_tag_80";
	private static final int k = 20;
	private static final int method = 1;
	private static final int[] times = {50, 100, 200, /*300,*/ 400, /*500, 600, 700,*/ 800, /*900, 1000, Integer.MAX_VALUE*/ 5000};
	private static final int lengthPrefixMinimum = 3;
	private static double coeff = 2.0f;

	public static void main(String[] args) throws IllegalArgumentException, ClassNotFoundException, SQLException{

		System.out.println("Experiments Beginning");
		TopKAlgorithm topk_alg;
		OptimalPaths optpath;
		BM25Score score = new BM25Score();

		if (args.length != 5) {
			System.out.println("Usage: java -jar -Xmx10000m executable.jar /path/to/files.txt numberOfDocuments networkFile inputTestFile outputFileName\nYou gave "+args.length+" parameters");
			for (int i=0; i<args.length; i++) {
				System.out.println("Argument "+(i+1)+": "+args[i]);
			}
			System.exit(0);
		}
		Params.dir = args[0];
		Params.number_documents = Integer.parseInt(args[1]);
		Params.networkFile = args[2];
		Params.inputTestFile = args[3];
		Params.outputTestFile = args[4];
		

		float alpha = 0f;

		//TODO clean the main loop, method for writing in xml, method to launch query more easily
		try {
			optpath = new OptimalPaths(network, null, heap, null, coeff);
			topk_alg = new TopKAlgorithm(null, taggers, network, method, score, alpha, pathFunction, optpath, 1);

			BufferedReader br = new BufferedReader(new FileReader(Params.dir+Params.inputTestFile));
			BufferedWriter bw = new BufferedWriter(new FileWriter(Params.dir+Params.outputTestFile));
			System.out.println("Initialisation done...");
			String line;
			String[] data;
			String user, item, tag;
			int numberUsersWhoTaggedThisItem;
			int lengthTag;
			HashSet<String> query;
			int ranking;
			int counter = 0;
			while ((line = br.readLine()) != null) {
				System.out.println("New line");
				data = line.split("\t");
				if (data.length != 4) {
					System.out.println("Wrong line in the input-file");
					continue;
				}
				user = data[0]; item = data[1]; tag = data[2];
				if (!Params.numberOfNeighbours.containsKey(user)) {
					System.out.println("User not connected...");
					continue;
				}
				if (Params.numberOfNeighbours.get(user) < 3) {
					System.out.println("Not enough friends...");
					continue;
				}
				lengthTag = tag.length();
				numberUsersWhoTaggedThisItem = Integer.parseInt(data[3]);
				for (alpha=0; alpha < 1.05 ; alpha+=0.25) {
					System.out.println("New alpha: "+alpha+" ...");
					topk_alg.setAlpha(alpha);
					for (int t: times) {
						query = new HashSet<String>();
						System.out.println("New time "+t+"...");
						for (int l=lengthPrefixMinimum; l<=lengthTag; l++) {
							//System.out.println("l94");
							if (query.isEmpty()) {
								query.add(tag.substring(0, l));
								//System.out.println("l97");
								topk_alg.executeQuery(user, query, k, t);
								//System.out.println("l99");
								ranking = topk_alg.getRankingItem(item, k);
								//System.out.println("l101");
								bw.write(user+"\t"+item+"\t"+tag+"\t"+numberUsersWhoTaggedThisItem+"\t"+t+"\t"+l+"\t"+alpha+"\t"+Params.threshold+"\t"+ranking+"\n");
							}
							else {
								query.remove(tag.substring(0, l-1));
								query.add(tag.substring(0, l));
								//System.out.println("l107");
								topk_alg.executeQueryPlusLetter(user, query, l, t);
								//System.out.println("l109");
								ranking = topk_alg.getRankingItem(item, k);
								//System.out.println("l111");
								bw.write(user+"\t"+item+"\t"+tag+"\t"+numberUsersWhoTaggedThisItem+"\t"+t+"\t"+l+"\t"+alpha+"\t"+Params.threshold+"\t"+ranking+"\n");
							}
						}
						topk_alg.reinitialize(tag.substring(0, lengthPrefixMinimum));
					}
				}
				counter++;
				System.out.println(counter+" lines processed...");
				if ((counter%20)==0) {
					System.gc();
					bw.flush();
				}
				if (counter > 600)
					break;
			}
			System.out.println(counter+" lines have been processed...");
			br.close();
			bw.close();
		} catch (SQLException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}