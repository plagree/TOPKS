package org.dbweb.Arcomem.Integration;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.dbweb.socialsearch.general.connection.PostgresqlConnection;
import org.dbweb.socialsearch.shared.Params;
import org.dbweb.socialsearch.shared.ResultsEncoding;
import org.dbweb.socialsearch.topktrust.algorithm.TopKAlgorithm;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathCompositionFunction;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathMultiplication;
import org.dbweb.socialsearch.topktrust.algorithm.paths.OptimalPaths;
import org.dbweb.socialsearch.topktrust.algorithm.score.BM25Score;
import org.dbweb.socialsearch.topktrust.datastructure.UserLink;

public class Experiments {

	private static final String[] queries = {
		//"opening",
		//"Obama",
		//"Syria"
		"SOUGOFOLLOW",
		"Apple",
		"NoMatter",
		/*"SOUGOF",
		"SOUGOFOL",
		"TFB",
		"TFB_TeamFollow",*/
		"TFB"
	};

	private static final String[][] seekers={
		{
			//"2", //TESTINGDB
			"168885306", //twitter dump
			//"238314320",
			//"30132505",
			//"20503",
			//"80087208"
		}
	};

	private static final float alpha = 0.0f;
	private static final boolean heap = true;
	private static final PathCompositionFunction pathFunction = new PathMultiplication();
	public static final String[] network = {"soc_snet_dt"};
	public static final String taggers = "soc_tag_80";//"tagging";
	private static final int k = 4;
	private static final int method = 1;
	private static final String metname = "exact";
	private static double coeff = 2.0f;

	public static void main(String[] args) throws IllegalArgumentException, ClassNotFoundException, SQLException{
		
		System.out.println("Let's go!!!!");
		TopKAlgorithm topk_alg;
		FileWriter xmlFile;
		OptimalPaths optpath;
		BM25Score score = new BM25Score();
		
		for (int i=0; i<args.length; i++) {
			Params.dir = args[i];
		}
		//TODO clean the main loop, method for writing in xml, method to launch query more easily
		try {
			int n = 0;

			// loop on all given NETWORKS
			for(int index_n=0; index_n<network.length;index_n++) {
				
				optpath = new OptimalPaths(network[index_n], null, heap, null, coeff);
				topk_alg = new TopKAlgorithm(null, taggers, network[index_n], method, score, alpha, pathFunction, optpath, 1);

				// loop on SEEKERS
				for(int index_s=0; index_s<seekers[index_n].length;index_s++) {

					// loop on QUERIES
					for(String q: queries) {
						HashSet<String> query = new HashSet<String>();
						query.add(q);
						n += 1;
						xmlFile = new FileWriter(String.format("tests_%s_%s_%s"+n+".xml", q, network[index_n], pathFunction.toString()));
						xmlFile.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
						xmlFile.write("<tests>");
						long timeBefore = System.currentTimeMillis();
						topk_alg.executeQuery(String.valueOf(seekers[index_n][index_s]), query, k); // TOPKS IS RUN HERE
						long timeAfter = System.currentTimeMillis();
						System.out.println("The algorithm ran in "+(timeAfter-timeBefore)/1000+" seconds with seeker "+seekers[index_n][index_s]);
						xmlFile.write(topk_alg.getResultsXML());
						xmlFile.write("</tests>");							
						xmlFile.close();

						/*xmlFile = new FileWriter(String.format("tests_%s_%s_%s"+n+".xml", q+"a", network[index_n], pathFunction.toString()));
						xmlFile.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
						xmlFile.write("<tests>");
						query.remove(q);
						query.add(q+"a");
						timeBefore = System.currentTimeMillis();
						topk_alg.executeQueryPlusLetter(String.valueOf(seekers[index_n][index_s]), query, k); // TOPKS IS RUN HERE
						timeAfter = System.currentTimeMillis();
						System.out.println("The algorithm ran in "+(timeAfter-timeBefore)/1000+" seconds with seeker "+seekers[index_n][index_s]);
						xmlFile.write(topk_alg.getResultsXML());
						xmlFile.write("</tests>");							
						xmlFile.close();*/
					}
				}
			}

		} catch (SQLException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
