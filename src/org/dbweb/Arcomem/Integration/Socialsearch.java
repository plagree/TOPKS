package org.dbweb.Arcomem.Integration;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.dbweb.Arcomem.datastructures.*;
import org.dbweb.socialsearch.general.connection.DBConnection;
import org.dbweb.socialsearch.general.connection.OracleConnection;
import org.dbweb.socialsearch.general.connection.PostgresqlConnection;
import org.dbweb.socialsearch.general.connection.SqliteConnection;
import org.dbweb.socialsearch.shared.Methods;
import org.dbweb.socialsearch.shared.Params;
import org.dbweb.socialsearch.topktrust.algorithm.TopKAlgorithm;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathCompositionFunction;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathMinimum;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathMultiplication;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathOne;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathPow;
import org.dbweb.socialsearch.topktrust.algorithm.paths.LandmarkPathsComputing;
import org.dbweb.socialsearch.topktrust.algorithm.paths.OptimalPaths;
import org.dbweb.socialsearch.topktrust.algorithm.score.BM25Score;
import org.dbweb.socialsearch.topktrust.algorithm.score.Score;
import org.dbweb.socialsearch.topktrust.algorithm.score.TfIdfScore;
import org.dbweb.socialsearch.topktrust.datastructure.Item;
import org.dbweb.socialsearch.topktrust.datastructure.UserLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.util.StringTokenizer;

public class Socialsearch implements SocialsearchInterface {
	private static Logger log = LoggerFactory.getLogger(Socialsearch.class);        



	private static String url;
	private static String login;
	private static String password;

	private static String network;
	private static String taggers;
	private static ArrayList<Integer> score;
	private static ArrayList<Integer> k;
	private static ArrayList<Integer> met;
	private static ArrayList<Integer> func;
	private static int qsize;
	private static ArrayList<ArrayList<String>> queries;
	private static ArrayList<ArrayList<String>> qrys;
	private static ArrayList<String> seekers;
	private static ArrayList<Double> alpha;
	private static ArrayList<Integer> num;
	private static double error;
	private static double coeff;

	private static HashMap<String,ArrayList<UserLink<String,Float>>> net;
	private static final int k1 = 2;
	private static PathCompositionFunction[] func_obj = {new PathOne(), new PathMinimum(), new PathMultiplication(), new PathPow()};
	private static final Score[] scores = {new BM25Score(), new TfIdfScore()};
	public static final String dateFormat = "yyyyMMdd_HHmm";
	private static DBConnection dbConn;

	private static ArrayList<String> processStringArray(String line){
		StringTokenizer st = new StringTokenizer(line, " ");
		ArrayList<String> list = new ArrayList<String>();
		while(st.hasMoreTokens()){
			list.add(st.nextToken());
		}
		return list;
	}

	private static ArrayList<Integer> processIntArray(String line){
		StringTokenizer st = new StringTokenizer(line, " ");
		ArrayList<Integer> list = new ArrayList<Integer>();
		while(st.hasMoreTokens()){
			list.add(Integer.parseInt(st.nextToken()));
		}
		return list;
	}

	private static ArrayList<Double> processDoubleArray(String line){
		StringTokenizer st = new StringTokenizer(line, " ");
		ArrayList<Double> list = new ArrayList<Double>();
		while(st.hasMoreTokens()){
			list.add(Double.parseDouble(st.nextToken()));
		}
		return list;
	}

	//TODO CONFIG FILE
	private static void readFile(String fileName){
		try {
			String pathToConfigFile = System.getProperty("user.dir")+"/Config/"+fileName;

			log.info("Reading file {}",pathToConfigFile);
			BufferedReader file = new BufferedReader(new FileReader(pathToConfigFile));
			//db credentials
			url = file.readLine();
			login = file.readLine();
			password = file.readLine();

			network = file.readLine();
			taggers = file.readLine();
			score = processIntArray(file.readLine());
			k = processIntArray(file.readLine());
			met = processIntArray(file.readLine());
			func = processIntArray(file.readLine());
			qsize = Integer.parseInt(file.readLine());
			queries = new ArrayList<ArrayList<String>>();
			for(int idx=0;idx<qsize;idx++)
				queries.add(processStringArray(file.readLine()));		    
			seekers = processStringArray(file.readLine());
			alpha = processDoubleArray(file.readLine());
			error = Double.parseDouble(file.readLine());
			coeff = Double.parseDouble(file.readLine());
			file.close();
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}


	private static void readRelFile(String fileName){
		try {
			log.info("Reading file {}",fileName);
			seekers = new ArrayList<String>();
			qrys = new ArrayList<ArrayList<String>>();
			num = new ArrayList<Integer>();
			BufferedReader file = new BufferedReader(new FileReader(fileName));
			String line = null;
			while((line=file.readLine())!=null){
				StringTokenizer st = new StringTokenizer(line,"\t");
				int idx = 0;
				while(st.hasMoreTokens()){
					String elem = st.nextToken();
					if(idx==0) seekers.add(elem);
					if(idx==1){
						ArrayList<String> qry = new ArrayList<String>();
						qry.add	(elem);
						qrys.add(qry);
					}
					if(idx==2){
						num.add(Integer.parseInt(elem));
					}
					idx++;
				}
			}
			file.close();
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	private static void optpath(){
		ArrayList<String> seekers = new ArrayList<String>();
		String seeker;
		String networkTable="";
		try {
			log.info("Reading file {}","seekers.csv");
			BufferedReader file = new BufferedReader(new FileReader("seekers.csv"));
			networkTable = file.readLine();
			coeff = Double.parseDouble(file.readLine());
			while((seeker=file.readLine())!=null)
				seekers.add(seeker);
			file.close();
		} catch (IOException e) {
			log.error(e.getMessage());
		}
		log.info("Computing optimal paths in {}",networkTable);
		OptimalPaths optp = new OptimalPaths(networkTable,dbConn,true,null,coeff);
		optp.computePaths(seekers);
	}

	private static void gen_views(){
		String sqlInsViews = "insert into view_queries(qid, seeker, alpha, func, scfunc, taggers, network) values(?,?,?,?,?,?,?)";
		String sqlInsViewQuery = "insert into view_keywords(qid,tag) values(?,?)";
		String sqlInsViewItems = "insert into view_items(qid,item, wscore, bscore) values(?,?,?,?)";
		String sqlGetMaxId = "select max(qid) from view_queries";

		log.info("Generating views on network {} and relation {}",network,taggers);
		OptimalPaths optpaths = new OptimalPaths(network,dbConn,true,null,coeff);
		Socialsearch.func_obj[3] = new PathPow(coeff);
		TopKAlgorithm topk_alg;
		Connection connection = dbConn.DBConnect();
		//Calendar cal = Calendar.getInstance();
		//SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		try {
			PreparedStatement ps = connection.prepareStatement(sqlGetMaxId);
			ResultSet rs = ps.executeQuery();
			int view_num = 0;
			if(rs.next())
				view_num = rs.getInt(1)+1;			
			for(int index_sc=0; index_sc<score.size(); index_sc++)
				for(int index_k=0; index_k<k.size();index_k++)
					for(int index_mt=0; index_mt<met.size();index_mt++)
						for(int index_f=0; index_f<func.size();index_f++)				
						{
							log.info("\tmethod {} function {}",String.format("met%s", met.get(index_mt)),func_obj[func.get(index_f)].toString());
							for(int index_q=0; index_q<queries.size();index_q++){
								String s_query = "";
								ArrayList<String> query = new ArrayList<String>();
								for(int idx_qq=0; idx_qq<queries.get(index_q).size();idx_qq++){									
									String p_query = queries.get(index_q).get(idx_qq);
									query.add(p_query);
									s_query += p_query + " ";
								}
								log.info("\t\tquery: {}",s_query);
								for(int index_s=0; index_s<seekers.size();index_s++)
									for(int index_a=0; index_a<alpha.size();index_a++)
									{
										log.info("\t\t\tseeker {} alpha {}",seekers.get(index_s),alpha.get(index_a));
										topk_alg = new TopKAlgorithm(dbConn, taggers, network, met.get(index_mt), scores[score.get(index_sc)], alpha.get(index_a).floatValue(), func_obj[func.get(index_f)], optpaths, error);
										topk_alg.executeQuery(String.valueOf(seekers.get(index_s)), query, k.get(index_k), Integer.MAX_VALUE,true);

										//
										ps = connection.prepareStatement(sqlInsViews);
										ps.setInt(1,view_num);
										ps.setInt(2,Integer.parseInt(seekers.get(index_s)));
										ps.setDouble(3,alpha.get(index_a).doubleValue());
										ps.setString(4, func_obj[func.get(index_f)].toString());
										ps.setString(5, scores[score.get(index_sc)].toString());
										ps.setString(6, taggers);
										ps.setString(7, network);
										ps.executeUpdate();
										ps = connection.prepareStatement(sqlInsViewQuery);
										for(String tg:query){
											ps.setInt(1, view_num);
											ps.setString(2, tg);
											ps.executeUpdate();
										}
										ps = connection.prepareStatement(sqlInsViewItems);
										for(Item it:topk_alg.getResults()){
											ps.setInt(1, view_num);
											ps.setString(2, String.valueOf(it.getItemId()));
											ps.setDouble(3, it.getComputedScore());
											ps.setDouble(4, it.getBestscore());
											ps.executeUpdate();
										}
										connection.commit();
										view_num++;

									}
							}					
						}
		} catch (SQLException ex) {
			log.error(ex.getMessage());
		}
	}

	private static void precision(){
		log.info("Plotting precision levels for {} and {}", network, taggers);
		OptimalPaths optpaths = new OptimalPaths(network,dbConn,true,null,coeff);
		Socialsearch.func_obj[3] = new PathPow(coeff);
		int num = 1000;
		TopKAlgorithm topk_alg;
		FileWriter resFile;
		try{
			int test_num = 0;
			for(int index_sc=0; index_sc<score.size(); index_sc++)
				for(int index_k=0; index_k<k.size();index_k++)
					for(int index_mt=0; index_mt<met.size();index_mt++)
						for(int index_f=0; index_f<func.size();index_f++)				
						{
							log.info("\tmethod {} function {}",String.format("met%s", met.get(index_mt)),func_obj[func.get(index_f)].toString());
							resFile = new FileWriter(String.format("prec/precision_%s_k%d_%s_%s_%s.csv", scores[score.get(index_sc)],k.get(index_k),String.format("met%s", met.get(index_mt)), network, func_obj[func.get(index_f)].toString()));
							double[][] precisions = new double[queries.size()][num]; 
							for(int index_q=0; index_q<qsize;index_q++){
								String s_query = "";
								ArrayList<String> query = new ArrayList<String>();
								for(int idx_qq=0; idx_qq<queries.get(index_q).size();idx_qq++){									
									String p_query = queries.get(index_q).get(idx_qq);
									query.add(p_query);
									s_query += p_query + " ";
								}
								log.info("\t\tquery: {}",s_query);
								for(int index_s=0; index_s<seekers.size();index_s++)
									for(int index_a=0; index_a<alpha.size();index_a++)
									{
										log.info("\t\t\tseeker {} alpha {} - exact",seekers.get(index_s),alpha.get(index_a));
										topk_alg = new TopKAlgorithm(dbConn, taggers, network, met.get(index_mt), scores[score.get(index_sc)], alpha.get(index_a).floatValue(), func_obj[func.get(index_f)], optpaths, error);
										topk_alg.executeQuery(String.valueOf(seekers.get(index_s)), query, k.get(index_k), Integer.MAX_VALUE,true);
										Set<String> exact = topk_alg.getTopKSet();
										precisions[index_q][0]=1.0f;
										int idx = 1;
										BufferedReader file = new BufferedReader(new FileReader(String.format("distr_gen/dist_%s_%s_%s.csv", seekers.get(index_s), network, func_obj[func.get(index_f)].toString())));
										String line = file.readLine();
										while(line!=null&&idx<num){
											String seeker = line.split("\t")[0];
											double prec = 1.0f;
											if(seeker!=null){
												log.info("\t\t\tseeker {} - approx",seeker);
												topk_alg.executeQuery(String.valueOf(seeker), query, k.get(index_k), Integer.MAX_VALUE,true);
												double common = 0f;
												for(String itm:topk_alg.getTopKSet()){													
													if(exact.contains(itm))
														common = common + 1.0f;
												}
												prec = common / (double)k.get(index_k);
											}
											precisions[index_q][idx] = prec;
											log.info(String.format("\t\t\t\tquery %d idx %d precision %.5f",index_q,idx,prec));
											idx++;
											line = file.readLine();
										}
										file.close();
									}
							}
							for(int i=0;i<num;i++){														
								double sum = 0;
								for(int j=0;j<queries.size();j++)
									sum += precisions[j][i];
								resFile.write(String.format("%d\t%.5f\n",i,sum/(double)queries.size()));
							}
							resFile.close();
						}
		} catch (IOException e) {
			log.error(e.getMessage());
		} catch (SQLException ex) {
			log.error(ex.getMessage());
		}
	}

	private static void relevance(){
		taggers = "soc_tag_80";
		log.info("Testing relevance on relation {}",taggers);

		Socialsearch.func_obj[0] = new PathMultiplication();
		Socialsearch.func_obj[1] = new PathMinimum();
		Socialsearch.func_obj[2] = new PathPow(1.1f);
		Socialsearch.func_obj[3] = new PathPow(2.0f);
		String[] nets = {"soc_snet_tt","soc_snet_d","soc_snet_dt"};
		Connection connection = dbConn.DBConnect();
		PreparedStatement ps = null;
		ResultSet result = null;
		TopKAlgorithm topk_alg;
		FileWriter csvFile;
		//Calendar cal = Calendar.getInstance();
		//SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
		try {
			for(int index_n=0; index_n<Params.network.length;index_n++){
				log.info("Testing on network {}",Params.network[index_n]);
				OptimalPaths optpaths = new OptimalPaths(Params.network[index_n],dbConn,true,null,coeff);
				csvFile = new FileWriter(String.format("relevance_%s.csv",nets[index_n]));
				for(int index=0; index<qrys.size();index++){

					String qry = "";
					HashSet<String> base = new HashSet<String>();
					boolean first = true;
					for(String tok:qrys.get(index)){
						ps = connection.prepareStatement("select distinct item from soc_tag_80 where tag=? and \"user\"=?");
						ps.setString(1, tok);
						ps.setInt(2, Integer.parseInt(seekers.get(index)));
						result = ps.executeQuery();
						HashSet<String> base_n = new HashSet<String>();
						while(result.next()){
							String itm = result.getString(1);
							if(!first&&(base.contains(itm))) base_n.add(itm);
							else if(first) base_n.add(itm);
						}
						result.close();
						ps.close();
						qry+=tok+" ";
						if(first) first=false;
						base = (HashSet<String>)base_n.clone();
					}
					log.info("\tseeker {} query {}",seekers.get(index),qry);
					//Get baseline (what's in the profile)
					ArrayList<Double> tp = new ArrayList<Double>();
					//Alpha = 0
					topk_alg = new TopKAlgorithm(dbConn, taggers, Params.network[index_n], Methods.MET_TOPKS, scores[1], 1, func_obj[0], optpaths, error);
					topk_alg.executeQuery(seekers.get(index), qrys.get(index), num.get(index), Integer.MAX_VALUE,true);
					int good = 0;
					for(String res:topk_alg.getTopKSet()){
						if(base.contains(res)) good++;
					}
					tp.add((double)good/(double)num.get(index));
					for(int index_f=0; index_f<func_obj.length;index_f++){
						topk_alg = new TopKAlgorithm(dbConn, taggers, Params.network[index_n], Methods.MET_TOPKS, scores[1], 0, func_obj[index_f], optpaths, error);
						topk_alg.executeQuery(seekers.get(index), qrys.get(index), num.get(index), Integer.MAX_VALUE,true);
						good = 0;
						for(String res:topk_alg.getTopKSet()){
							if(base.contains(res)) good++;
						}
						tp.add((double)good/(double)num.get(index));

					}
					String output=String.format("%d\t%s\t",seekers.get(index),qry);
					for(Double val:tp) output+=String.format("%.5f\t", val);
					csvFile.write(output+"\n");
				}
				csvFile.close();
			}
		} catch (IOException e) {
			log.error(e.getMessage());
		} catch (SQLException ex) {
			log.error(ex.getMessage());
		}
	}



	private static void test(){
		log.info("Testing network {} on relation {}",network,taggers);
		OptimalPaths optpaths = new OptimalPaths(network,dbConn,true,null,coeff);



		LandmarkPathsComputing landmark = new LandmarkPathsComputing(network,dbConn);
		Socialsearch.func_obj[3] = new PathPow(coeff);

		String sqlGetNumberDocuments = "select * from status";
		int number_documents=0 ;
		int number_users=0;

		PreparedStatement ps = null;
		ResultSet result= null;

		Connection connection = dbConn.DBConnect(url, login, password);
		try {
			log.info("connecting to DB {}", url);
			ps = connection.prepareStatement(sqlGetNumberDocuments);

			result = ps.executeQuery();
			while(result.next()){
				number_users = result.getInt(1);
				number_documents = result.getInt(2);
			}
			result.close();
			ps.close();

			TopKAlgorithm topk_alg;
			FileWriter xmlFile;
			//Calendar cal = Calendar.getInstance();
			//SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);

			landmark.loadLandmarks();

			ArrayList<String> query = new ArrayList<String>();
			query.add("Syria"); 
			//Dummy query for loading the networks
			topk_alg = new TopKAlgorithm(dbConn, taggers, network, Methods.MET_TOPKS, new TfIdfScore(), 0.0f, new PathMultiplication(), optpaths, error, number_documents, number_users);
			topk_alg.setLandmarkPaths(landmark);
			topk_alg.executeQuery("53150930", query, 1, Integer.MAX_VALUE,true);

			int test_num = 0;
			for(int index_sc=0; index_sc<score.size(); index_sc++)
				for(int index_k=0; index_k<k.size();index_k++)
					for(int index_mt=0; index_mt<met.size();index_mt++)
						for(int index_f=0; index_f<func.size();index_f++)				
						{
							log.info("\tmethod {} function {}",String.format("met%s", met.get(index_mt)),func_obj[func.get(index_f)].toString());
							xmlFile = new FileWriter(String.format("tests_%s_k%d_%s_%s_%s.xml", scores[score.get(index_sc)],k.get(index_k),String.format("met%s", met.get(index_mt)), network, func_obj[func.get(index_f)].toString()));
							xmlFile.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
							xmlFile.write("<tests>");
							for(int index_q=0; index_q<qsize;index_q++){
								String s_query = "";
								query = new ArrayList<String>();
								for(int idx_qq=0; idx_qq<queries.get(index_q).size();idx_qq++){									
									String p_query = queries.get(index_q).get(idx_qq);
									query.add(p_query);
									s_query += p_query + " ";
								}
								log.info("\t\tquery: {}",s_query);
								for(int index_s=0; index_s<seekers.size();index_s++)
									for(int index_a=0; index_a<alpha.size();index_a++)
									{
										log.info("\t\t\tseeker {} alpha {}",seekers.get(index_s),alpha.get(index_a));
										topk_alg = new TopKAlgorithm(dbConn, taggers, network, met.get(index_mt), scores[score.get(index_sc)], alpha.get(index_a).floatValue(), func_obj[func.get(index_f)], optpaths, error, number_documents, number_users);
										topk_alg.setLandmarkPaths(landmark);
										topk_alg.executeQuery(String.valueOf(seekers.get(index_s)), query, k.get(index_k), Integer.MAX_VALUE,true);
										xmlFile.write(topk_alg.getResultsXML());
									}
							}
							xmlFile.write("</tests>");							
							xmlFile.close();
						}
		} catch (IOException e) {
			log.error(e.getMessage());
		} catch (SQLException ex) {
			log.error(ex.getMessage());
		}
	}
	public Socialsearch() {

	}



	@Override
	public BasicSearchResult BasicSearch(GeneralConfigFile cfile, CrawlID crawlid, Keywords keywords) {
		/*
		 * properties for DB connection and keyword query
		 * dbProps contains: dbname, login, passwd
		 * qryProps contains: k
		 */
		Properties dbProps = new Properties();
		Properties qryProps = new Properties();



		BasicSearchResult res = new BasicSearchResult();
		try{
			/*
			 * first open DB properties file
			 */
			FileInputStream in = new FileInputStream(cfile.getPathToFile());
			dbProps.loadFromXML(in);
			in.close();


			//Connection to the database
			if(dbProps.getProperty("topks.db.driver").equals("oracle"))
			{
				dbConn = new OracleConnection(dbProps.getProperty("topks.db.url"),dbProps.getProperty("topks.db.user"),dbProps.getProperty("topks.db.password"));
			}
			else if(dbProps.getProperty("topks.db.driver").equals("pgsql")){
				dbConn = new PostgresqlConnection(dbProps.getProperty("topks.db.url"),dbProps.getProperty("topks.db.user"),dbProps.getProperty("topks.db.password"));
			}
			else if(dbProps.getProperty("topks.db.driver").equals("sqlite")){
				dbConn = new SqliteConnection(dbProps.getProperty("topks.db.url"));
			}
			else{
				log.error("No supported database specified.");
				System.exit(1);
			}

			/*
			 * network resp. taggers indicate which table in the database should be read
			 * Default: soc_snet_d for network, soc_tag_80
			 */
			String network = Test.network[0];	//"soc_snet_d";
			String taggers = Test.taggers;	//"soc_tag_80";
			int method = Methods.MET_TOPKS;
			float coeff = 1.1f;
			TopKAlgorithm topk_alg;


			OptimalPaths optpaths = new OptimalPaths(network,dbConn,true,null,coeff);

			/*
			 * The lines below have been commented for the sake of the integration within ARCOMEM Framework
			 * Indeed, landmark computations go beyond the scope of this project.
			 */
			//		LandmarkPathsComputing landmark = new LandmarkPathsComputing("soc_snet_d",dbConn);
			//		landmark.loadLandmarks();
			topk_alg = new TopKAlgorithm(dbConn, taggers, network, method, new TfIdfScore(), 0.0f, new PathMultiplication(), optpaths, 0.9f);
			//		topk_alg.setLandmarkPaths(landmark);
			//		 fetch seeker from table 
			Connection connection = dbConn.DBConnect();
			connection.setAutoCommit(false);

			/*
			 * The lines below are commented since seeker shoul be provided in input.
			 * If it is not the case, it is retrieved from the DB.
			 */
			//		String seeker = qryProps.getProperty("topks.query.seeker","71393");


			/*
			 * retrieve seeker from DB
			 */
			ResultSet seekerList;
			PreparedStatement ps = connection.prepareStatement("SELECT seeker from  seekers where score = (select max(score) from seekers)");

			seekerList = ps.executeQuery();	

			String seeker="";
			if (seekerList.next())
			{
				seeker=seekerList.getString(1);

			}		
			log.info("seeker = {}",seeker);



			int k = Integer.parseInt(qryProps.getProperty("topks.query.k","10"));
			/*
			 * call the social search functionality directly through the executeQuery() method
			 */
			topk_alg.executeQuery(seeker, keywords.getQuery(), k, Integer.MAX_VALUE, true);

			res = topk_alg.getResultsList();

		}

		catch(SQLException sql_ex){
			log.error("Error in SQL: "+sql_ex.getMessage());
		}
		catch(Exception ex){
			log.error("Error == : "+ex.getMessage());
			ex.printStackTrace();
		}

		return res;

	}

	@Override
	public BasicSearchResult BasicSearch(GeneralConfigFile cfgFile,
			CrawlID crawlid, Keywords keywords, Seeker seek) {
		Properties dbProps = new Properties();
		Properties qryProps = new Properties();

		BasicSearchResult res = new BasicSearchResult();
		try{
			FileInputStream in = new FileInputStream(cfgFile.getPathToFile());
			dbProps.loadFromXML(in);
			in.close();


			//Connection to the database
			if(dbProps.getProperty("topks.db.driver").equals("oracle"))
			{
				dbConn = new OracleConnection(dbProps.getProperty("topks.db.url"),dbProps.getProperty("topks.db.user"),dbProps.getProperty("topks.db.password"));
			}
			else if(dbProps.getProperty("topks.db.driver").equals("pgsql")){
				dbConn = new PostgresqlConnection(dbProps.getProperty("topks.db.url"),dbProps.getProperty("topks.db.user"),dbProps.getProperty("topks.db.password"));
			}
			else if(dbProps.getProperty("topks.db.driver").equals("sqlite")){
				dbConn = new SqliteConnection(dbProps.getProperty("topks.db.url"));
			}
			else{
				log.error("No supported database specified.");
				System.exit(1);
			}

			String network = "soc_snet_d";
			String taggers = "soc_tag_80";
			int method = Methods.MET_TOPKS;
			float coeff = 1.1f;
			TopKAlgorithm topk_alg;


			OptimalPaths optpaths = new OptimalPaths(network,dbConn,true,null,coeff);

			topk_alg = new TopKAlgorithm(dbConn, taggers, network, method, new TfIdfScore(), 0.0f, new PathMultiplication(), optpaths, 0.9f);
			//		topk_alg.setLandmarkPaths(landmark);
			//		 fetch seeker from table 
			Connection connection = dbConn.DBConnect();
			connection.setAutoCommit(false);

			int k = Integer.parseInt(qryProps.getProperty("topks.query.k","10"));
			topk_alg.executeQuery(seek.getUserId(), keywords.getQuery(), k, Integer.MAX_VALUE,true);
			res = topk_alg.getResultsList();
		}

		catch(SQLException sql_ex){
			log.error("Error in SQL: "+sql_ex.getMessage());
		}
		catch(Exception ex){
			log.error("Error == : "+ex.getMessage());
			ex.printStackTrace();
		}
		return res;
	}

}	
