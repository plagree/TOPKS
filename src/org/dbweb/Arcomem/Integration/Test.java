package org.dbweb.Arcomem.Integration;


import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.dbweb.socialsearch.general.connection.PostgresqlConnection;
import org.dbweb.socialsearch.shared.ResultsEncoding;
import org.dbweb.socialsearch.topktrust.algorithm.TopKAlgorithm;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathCompositionFunction;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathMultiplication;
import org.dbweb.socialsearch.topktrust.algorithm.paths.OptimalPaths;
import org.dbweb.socialsearch.topktrust.algorithm.score.BM25Score;
import org.dbweb.socialsearch.topktrust.datastructure.UserLink;

public class Test{
	public static final String dateFormat = "yyyyMMdd_HHmm";
	private static PostgresqlConnection dbConn = new PostgresqlConnection("localhost:5432/twitter", "postgres", "postgrespass");
	private static HashMap<String,ArrayList<UserLink<String,Float>>> net;
	
	//test settings
	private static final PathCompositionFunction[] func = {	new PathMultiplication()};

	private static final String[] query1 = {
		//"car", //testindb
		//"Obama", //twitter dump
		//"TFBJP",
		//"Cancer",
		//"Syria",
		//"SOUGOFOLLOW",
		//"Apple",
		"SOUGOF",
		"SOUGOFOL",
		//"openingact",
		//"openingceremony",
		//"opening"
	};

	private static final String[][] seekers={
		{
			//"2", //TESTINGDB
			"168885306", //twitter dump
			//"30132505",
			//"20503",
			//"80087208"
		}
	};

	private static final float[] alpha ={0.0f};
	private static final boolean[] heap = {true};
	public static final String[] network = {"soc_snet_dt"};
	public static final String taggers = "soc_tag_80";//"tagging";
	private static final int k = 4;
	private static final int[] met = {1};//,1,2,4};
	private static final String[] metname = {"exact"};//,"met1","met2","met4"};
	private static double coeff = 2.0f;

	public static void main(String[] args) throws IllegalArgumentException, ClassNotFoundException, SQLException{

		String[] res = {"No results yet"};
		ResultsEncoding results = new ResultsEncoding();
		results.setResults(res);
		TopKAlgorithm topk_alg;
		FileWriter xmlFile;
		OptimalPaths optpath;
		BM25Score score = new BM25Score();

		//ComplexSearchResult complexTopkList = new   ComplexSearchResult();

		try {
			//ArrayList<BasicSearchResult> manyTopks=new ArrayList<BasicSearchResult>();
			int test_num = 0;
			int n = 0;

			// loop on all given NETWORKS
			for(int index_n=0; index_n<network.length;index_n++){ 
				net = null;
				optpath = new OptimalPaths(network[index_n],dbConn,true,null,coeff);

				// loop on all given ALGORITHMS (exact, ...)
				for(int index_mt=0; index_mt<met.length;index_mt++){

					// loop on all given PATH_COMPOSITION_FUNCTIONS
					for(int index_f=0; index_f<func.length;index_f++){

						// loop on heap (actually, always heap)
						for(int index_m=0; index_m<heap.length;index_m++) {

							// loop on ALPHA VALUES
							for(int index_a=0; index_a<alpha.length;index_a++) {
								
								topk_alg = new TopKAlgorithm(dbConn, taggers, network[index_n], met[index_mt], score, alpha[index_a], func[index_f], optpath, 1);
								
								// loop on SEEKERS
								for(int index_s=0; index_s<seekers[index_n].length;index_s++) {
									
									// loop on QUERIES
									for(int index_q=0; index_q<query1.length;index_q++) { 
										HashSet<String> query = new HashSet<String>();
										query.add(query1[index_q]);
										n += 1;
										xmlFile = new FileWriter(String.format("tests_%s_%s_%s"+n+".xml", metname[index_mt], network[index_n], func[index_f].toString()));
										xmlFile.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
										xmlFile.write("<tests>");
										
										long timeBefore = System.currentTimeMillis();
										topk_alg.executeQuery(String.valueOf(seekers[index_n][index_s]), query, k); // TOPKS IS RUN HERE
										long timeAfter = System.currentTimeMillis();
										System.out.println("The algorithm ran in "+(timeAfter-timeBefore)/1000+" seconds with seeker "+seekers[index_n][index_s]);
										xmlFile.write(topk_alg.getResultsXML());
										test_num++;
										res[0] = String.format("Currently at test number %d...",test_num);
										results.setResults(res);
										xmlFile.write("</tests>");							
										xmlFile.close();
									}
								}
							}
						}
					}
				}
			}
		} catch (IOException e) {
			res[0]= e.getMessage();
			results.setResults(res);
		}
		catch (SQLException ex) {
			res[0]= ex.getMessage();
			results.setResults(res);
		}
	}
}