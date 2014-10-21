package org.dbweb.Arcomem.Integration;


import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.dbweb.Arcomem.datastructures.BasicSearchResult;
import org.dbweb.Arcomem.datastructures.ComplexSearchResult;
import org.dbweb.Arcomem.datastructures.Seeker;
import org.dbweb.socialsearch.general.connection.PostgresqlConnection;
import org.dbweb.socialsearch.shared.ResultsEncoding;
import org.dbweb.socialsearch.topktrust.algorithm.TopKAlgorithm;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathCompositionFunction;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathMultiplication;
import org.dbweb.socialsearch.topktrust.algorithm.paths.OptimalPaths;
import org.dbweb.socialsearch.topktrust.algorithm.score.BM25Score;
import org.dbweb.socialsearch.topktrust.datastructure.UserLink;
import org.postgresql.Driver;

import com.ibm.icu.util.Calendar;

public class Test{
	public static final String dateFormat = "yyyyMMdd_HHmm";
	private static PostgresqlConnection dbConn = new PostgresqlConnection("localhost:5432/testingdb", "postgres", "paul");
	private static HashMap<String,ArrayList<UserLink<String,Float>>> net;

	//test settings
	//private static final PathCompositionFunction[] func = {new PathOne()};
	//	private static final PathCompositionFunction[] func = {new PathMinimum(), new PathMultiplication(), new PathPow()};
	private static final PathCompositionFunction[] func = {	new PathMultiplication()};

	private static final String[] query1 = {
		"car",
	};
	//	private static final String[] query2 = {"book","food","articles","tech","game"};

	private static final String[][] seekers={
		{
			"2",
			//"168885306",
			//"80087208"
		}
	};
	//	private static final String[][] seekers2 = {
	//{"70198", "56682", "43637", "57175", "33257", "2939", "37982", "52354", "25158", "61691"},
	//{"6053", "7594", "18503", "21629", "8693", "37373", "42733", "79529", "78762", "65915"},
	//	};

	private static final float[] alpha ={0.0f};
	private static final boolean[] heap = {true};
	public static final String[] network = {"soc_snet_tt"};
	public static final String taggers = "tagging";//"soc_tag_80";
	private static final int k1 = 2;
	private static final int k = 10;
	private static final int[] met = {0};//,1,2,4};
	private static final String[] metname = {"exact"};//,"met1","met2","met4"};
	private static double coeff = 2.0f;
	private static String r_preporc = String.format("%s%n%s","require(gtools)","require(RobustRankAggreg)");

	public static void main(String[] args) throws IllegalArgumentException, ClassNotFoundException, SQLException{

		//		Class.forName("postgresql.jdbc.driver.PostgresqlDriver");
		//		System.exit(0);

		String[] res = {"No results yet"};
		ResultsEncoding results = new ResultsEncoding();
		results.setResults(res);
		TopKAlgorithm topk_alg;
		FileWriter xmlFile;
		FileWriter rFile;
		//Calendar cal = Calendar.getInstance();
		//SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		OptimalPaths optpath;
		BM25Score score = new BM25Score();
		//

		//ComplexSearchResult complexTopkList = new   ComplexSearchResult();

		try {
			//ArrayList<BasicSearchResult> manyTopks=new ArrayList<BasicSearchResult>();

			int test_num = 0;
			for(int index_n=0; index_n<network.length;index_n++){ 
				net = null;
				optpath = new OptimalPaths(network[index_n],dbConn,true,null,coeff);
				for(int index_mt=0; index_mt<met.length;index_mt++){
					for(int index_f=0; index_f<func.length;index_f++){

						xmlFile = new FileWriter(String.format("tests_%s_%s_%s.xml", metname[index_mt], network[index_n], func[index_f].toString()));
						rFile = new FileWriter(String.format("RFile_%s_%s_%s.r", metname[index_mt], network[index_n], func[index_f].toString()));
						// initialize
						rFile.write(r_preporc);
						xmlFile.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
						xmlFile.write("<tests>");
						// rFile.write("#Results");TODO

						for(int index_m=0; index_m<heap.length;index_m++)
							for(int index_q=0; index_q<query1.length;index_q++) //FOR EACH QUERY
							{
								HashSet<String> query = new HashSet<String>();
								query.add(query1[index_q]); 
								// query.add(query2[index_q]);
								for(int index_s=0; index_s<seekers[index_n].length;index_s++) //FOR EACH SEEKER
									for(int index_a=0; index_a<alpha.length;index_a++)
									{
										topk_alg = new TopKAlgorithm(dbConn, taggers, network[index_n], met[index_mt], score, alpha[index_a], func[index_f], optpath, 1);
										topk_alg.executeQuery(String.valueOf(seekers[index_n][index_s]), query, k);
										xmlFile.write(topk_alg.getResultsXML());
										test_num++;
										res[0] = String.format("Currently at test number %d...",test_num);
										results.setResults(res);

										//concatenate to current topk TODO
										//										manyTopks.add(topk_alg.getResultsList());

										//commenter on 8/9/14
										//										complexTopkList.addToResults(new Seeker(seekers[index_n][index_s]), topk_alg.getResultsList());
									}
								//write once for all topklists
								//								rFile.write(complexTopkList.toRMatrices());
								//aggregate lists 
								//								rFile.write("aggregateRanks(nlist)");
								//retrieve results
							}
						xmlFile.write("</tests>");							
						xmlFile.close();
					}
				}
			}	
		} catch (IOException e) {
			res[0]= e.getMessage();
			results.setResults(res);
		} catch (SQLException ex) {
			res[0]= ex.getMessage();
			results.setResults(res);
		}
	}
}