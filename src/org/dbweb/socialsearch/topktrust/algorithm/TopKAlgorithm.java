/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbweb.socialsearch.topktrust.algorithm;


import org.apache.maven.artifact.repository.metadata.Metadata;
import org.dbweb.Arcomem.datastructures.BasicSearchResult;
import org.dbweb.Arcomem.datastructures.Interval;
import org.dbweb.Arcomem.datastructures.ItemID;
import org.dbweb.Arcomem.datastructures.TopksMetadata;
import org.dbweb.socialsearch.general.connection.DBConnection;
import org.dbweb.socialsearch.shared.Methods;
import org.dbweb.socialsearch.shared.Params;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathCompositionFunction;
import org.dbweb.socialsearch.topktrust.algorithm.paths.LandmarkPathsComputing;
import org.dbweb.socialsearch.topktrust.algorithm.paths.OptimalPaths;
import org.dbweb.socialsearch.topktrust.algorithm.score.Score;
import org.dbweb.socialsearch.topktrust.datastructure.DataDistribution;
import org.dbweb.socialsearch.topktrust.datastructure.DataHistogram;
import org.dbweb.socialsearch.topktrust.datastructure.Item;
import org.dbweb.socialsearch.topktrust.datastructure.ItemList;
import org.dbweb.socialsearch.topktrust.datastructure.UserEntry;
import org.dbweb.socialsearch.topktrust.datastructure.UserLink;
import org.dbweb.socialsearch.topktrust.datastructure.comparators.MinScoreItemComparator;
import org.dbweb.socialsearch.topktrust.datastructure.general.SortedQueue;
import org.dbweb.socialsearch.topktrust.datastructure.views.UserView;
import org.dbweb.socialsearch.topktrust.datastructure.views.ViewScore;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.util.Calendar;

/**
 *
 * @author Silver
 */
public class TopKAlgorithm{

	//debug purpose
	public ArrayList<Integer> visitedNodes;

	public static Logger log = LoggerFactory.getLogger(TopKAlgorithm.class);

	protected String pathToQueries = System.getProperty("user.dir")+"/queries/normal/";
	protected String pathToDistributions = System.getProperty("user.dir")+"/distr/";

	protected static double viewDistanceThreshold = 0.5;

	protected static String sqlGetNeighboursTemplate = "select user2,weight from %s where user1=?";
	protected static String sqlGetDocumentsTemplate = "select tag,item from %s where \"user\"=? and (";
	protected static String sqlGetNumberTaggersTemplate = "select count(distinct \"user\") from %s where item=? and tag=?";
	protected static String sqlGetDistributionTemplate = "select mean, var from stats_%s where \"user\"=? and func=?";
	protected static String sqlGetHistogramTemplate = "select bucket, num from hist_%s where \"user\"=? and func=? order by bucket asc";
	protected static String sqlGetTagFrequency = "select num from tagfreq where tag=?";
	protected static String sqlGetDocumentTf = "select num from docs where item=? and tag=?";
	protected static String sqlGetNumberUsersTemplate = "select count(distinct \"user\") from %s";
	protected static String sqlGetNumberDocumentsTemplate = "select count(distinct item) from %s";
	protected static String sqlGetDocsListByTag = "select item,num from docs where tag=? order by num desc";    
	protected static String sqlGetTaggersTemplate = "select \"user\" from %s where tag in (";
	protected static String sqlGetAllDocumentsTemplate = "select * from %s where tag in (";
	protected static String sqlAddQueryTerm = "tag=\'%s\'";
	protected static String sqlGetViewQuery = "select tag from view_keywords where qid=? order by tag asc";
	protected static String sqlGetViewsTemplate = "select distinct q.seeker, q.qid, q.alpha, q.coeff from view_queries q, view_keywords k where k.qid=q.qid and q.func=? and q.scfunc=? and q.network=?  and q.coeff=? and q.hidden=0 and k.tag in (";

	protected String sqlGetDocuments;
	protected String sqlGetTaggers;
	protected String sqlGetAllDocuments;

	protected int k1 = 0;    

	protected String sqlGetNeighbours;


	protected String networkTable;
	protected String tagTable;

	protected Connection connection;

	protected ItemList candidates;


	public ItemList getPubCandids() {
		return this.candidates;

	}

	protected HashMap<Integer,HashMap<String,HashSet<String>>> docs_users;

	protected HashMap<String,Float> tag_idf;
	protected HashMap<String, ListIterator<UserEntry<Float>>> friends_list;
	protected ArrayList<UserEntry<Float>> friends;
	protected ArrayList<Float> values;
	protected PriorityQueue<UserEntry<Float>> prioQueue;
	protected HashMap<String,Integer> high_docs;
	protected HashMap<String,Float> positions;
	protected HashMap<String,Float> userWeights;
	protected ArrayList<Double> proximities;
	protected HashMap<String,Integer> tagFreqs;
	protected HashMap<String,Integer> lastpos;
	protected HashMap<String,Float> lastval;
	protected HashMap<String,Float> maxval;
	protected HashSet<String> taggers;
	protected HashMap<String,ArrayList<UserView>> userviews;
	protected HashMap<String,HashSet<String>> unknown_tf;
	protected ArrayList<Integer> vst;
	protected HashSet<Integer> skr;
	protected String[] next_docs;
	protected ResultSet[] docs;

	protected int[] pos;
	protected int seeker;

	protected float userWeight;
	protected UserEntry<Float> currentUser;

	protected DBConnection dbConnection;

	protected PathCompositionFunction distFunc;    

	protected boolean terminationCondition;

	protected long time_heapinit;
	protected long time_preinit;
	protected long time_loop;
	protected long time_queries;
	protected long time_clist;
	protected long time_term;
	protected long time_heap;
	protected long time_dji;
	protected long time_dat;
	protected long time_rel;

	protected int total_documents_social;
	protected int total_documents_asocial;
	protected int total_users;
	protected int total_rnd;
	protected int total_topk_changes;
	protected int total_conforming_lists;    
	protected int total_memory_seeks;
	protected int total_heap_adds;
	protected int total_heap_rebuilds;
	protected int total_heap_interchanges;

	protected int number_documents;
	protected int number_users;

	protected float partial_sum = 0;
	protected float total_sum = 0;

	protected float alpha = 0;

	protected HashSet<UserEntry<Float>> done;
	protected HashMap<String,ArrayList<UserLink<String,Float>>> network;

	protected int total_lists_social;
	protected boolean heap;

	protected boolean foundFirst;		
	protected Iterator<UserLink<String,Float>> iter;

	//	protected String resultsXML;

	//amine
	protected String newXMLResults="", newBucketResults="", newXMLStats="";
	BasicSearchResult resultList=new BasicSearchResult();
	double [] scoreInt = new double[2];

	public BasicSearchResult getResultList() {
		resultList.sortItems();
		return resultList;
	}



	protected int approxMethod;
	protected float max_pos_val;

	private OptimalPaths optpath;
	private LandmarkPathsComputing landmark;
	private DataDistribution d_distr;
	private DataHistogram d_hist;
	private ViewTransformer viewTransformer;
	private Score score;
	private double error;
	private Item<String> virtualItem;

	private double bestScoreEstim = Double.POSITIVE_INFINITY; 
	//debug purpose
	public double bestscore;

	private boolean docs_inserted;
	private boolean finished;
	private boolean all_landmarks;

	private boolean firstPossible = true;
	private boolean needUnseen = true;
	private boolean skipViews = true;
	private HashSet<String> guaranteed;
	private HashSet<String> possible;

	private int numloops=0; //amine

	public TopKAlgorithm(DBConnection dbConnection, String tagTable, String networkTable, int method, Score itemScore, float scoreAlpha, PathCompositionFunction distFunc, OptimalPaths optPathClass, double error){
		//super(distFunc, dbConnection, networkTable, tagTable);
		this.distFunc = distFunc;
		this.dbConnection = dbConnection;
		this.networkTable = networkTable;
		this.tagTable = tagTable;
		this.alpha = scoreAlpha;
		this.approxMethod = method;
		// HashMap<String,ArrayList<UserLink<String,Float>>> network
		// this.network = network;
		this.optpath = optPathClass;
		this.error = error;
		this.score = itemScore;
		this.number_documents = 595811;
		this.number_users = 80000;
	}

	public TopKAlgorithm(DBConnection dbConnection, String tagTable, String networkTable, int method, Score itemScore, float scoreAlpha, PathCompositionFunction distFunc, OptimalPaths optPathClass, double error, int number_documents, int number_users){
		//super(distFunc, dbConnection, networkTable, tagTable);
		log.info("dbconn{}", dbConnection);
		this.distFunc = distFunc;
		this.dbConnection = dbConnection;
		this.networkTable = networkTable;
		this.tagTable = tagTable;
		this.alpha = scoreAlpha;
		this.approxMethod = method;
		// HashMap<String,ArrayList<UserLink<String,Float>>> network
		// this.network = network;
		this.optpath = optPathClass;
		this.error = error;
		this.score = itemScore;
		this.number_documents = number_documents;
		this.number_users = number_users;
	}

	/*
	 * OLD CODE NOT USED
	 * 
	public int basicexecuteQuery(String seeker, HashSet<String> query, int k, int maxLoops) throws SQLException{
		System.out.println("270");
		this.time_dji = 0;
		this.time_term = 0;
		this.time_clist = 0;
		this.time_queries = 0;
		this.time_heap = 0;
		this.time_dat = 0;
		this.max_pos_val = 1.0f;
		this.d_distr = null;
		this.d_hist = null;
		this.total_users = 0;
		this.total_rnd = 0;
		this.seeker = Integer.parseInt(seeker);
		vst = new ArrayList<Integer>();

		sqlGetNeighbours = String.format(sqlGetNeighboursTemplate, this.networkTable);
		values = new ArrayList<Float>();
		taggers = new HashSet<String>();
		unknown_tf = new HashMap<String,HashSet<String>>();
		// userviews = new HashMap<String,ArrayList<UserView>>();
		for(String tag:query)
			unknown_tf.put(tag, new HashSet<String>());    	
		connection = dbConnection.DBConnect();
		connection.setAutoCommit(false);
		this.optpath.setValues(values);
		this.optpath.setDistFunc(distFunc);
		long time = System.currentTimeMillis();

		if((this.approxMethod&Methods.MET_APPR_LAND)==Methods.MET_APPR_LAND){
			landmark.setSeeker(this.seeker);
			landmark.setPathFunction(this.distFunc);
			currentUser = new UserEntry<Float>(this.seeker,1.0f);
		}
		else{
			currentUser = optpath.initiateHeapCalculation(this.seeker, query);
		}
		time_heapinit = System.currentTimeMillis() - time;
		userWeight = 1.0f;


		terminationCondition = false;

		PreparedStatement ps;
		ResultSet result;                

		//        String sqlGetNumberDocuments = String.format(sqlGetNumberDocumentsTemplate, this.tagTable);
		//        String sqlGetNumberUsers = String.format(sqlGetNumberUsersTemplate, this.tagTable);

		time = System.currentTimeMillis();
		//Establishing the global properties

		//        ps = connection.prepareStatement(sqlGetNumberDocuments);
		//    	result = ps.executeQuery();
		//    	while(result.next()){
		//    		this.number_documents = result.getInt(1);
		//    	}
		//    	ps = connection.prepareStatement(sqlGetNumberUsers);
		//    	result = ps.executeQuery();
		//    	while(result.next()){
		//    		this.number_users = result.getInt(1);
		//    	}


		//Preparing the docs list

		high_docs = new HashMap<String,Integer>();
		positions = new HashMap<String,Float>();
		userWeights = new HashMap<String,Float>();
		proximities = new ArrayList<Double>();
		tagFreqs = new HashMap<String,Integer>();
		lastpos = new HashMap<String,Integer>();
		lastval = new HashMap<String,Float>();    	    
		tag_idf = new HashMap<String,Float>();
		next_docs = new String[query.size()];
		pos = new int[query.size()];
		docs = new ResultSet[query.size()];
		int index = 0;
		for(String tag:query){
			ps = connection.prepareStatement(sqlGetDocsListByTag);
			ps.setString(1, tag);
			docs[index] = ps.executeQuery();
			if(docs[index].next()){
				high_docs.put(tag, docs[index].getInt(2));
				next_docs[index] = docs[index].getString(1);
			}
			else{
				high_docs.put(tag, 0);
				next_docs[index] = "";
			}
			positions.put(tag, 0f);
			userWeights.put(tag, userWeight);
			pos[index]=0;
			index++;
			ps = connection.prepareStatement(sqlGetTagFrequency);
			ps.setString(1, tag);
			result = ps.executeQuery();
			int tagfreq = 0;
			if(result.next()) tagfreq = result.getInt(1);
			tagFreqs.put(tag, high_docs.get(tag));
			float tagidf = (float) Math.log(((float)this.number_documents - (float)tagfreq + 0.5)/((float)tagfreq+0.5));
			this.tag_idf.put(tag, new Float(tagidf));
		}
		proximities.add((double)userWeight);

		//getting the userviews
		String sqlGetViews = sqlGetViewsTemplate;
		int idx=0;
		for(String tag:query){
			if(idx<query.size()-1){
				sqlGetViews+=String.format("\'%s\',", tag);
			}
			else{
				sqlGetViews+=String.format("\'%s\')", tag);
			}
			idx++;
		}

		ps = connection.prepareStatement(sqlGetViews);
		ps.setString(1, distFunc.toString());
		ps.setString(2, score.toString());
		ps.setString(3, networkTable);
		ps.setDouble(4, distFunc.getCoeff());
		result = ps.executeQuery();
		while(result.next()){
			String seek = result.getString(1);
			int qid = result.getInt(2);
			double alph = result.getFloat(3);
			if(!userviews.containsKey(seek))
				userviews.put(seek, new ArrayList<UserView>());
			UserView uview = new UserView();
			HashSet<String> vqry = new HashSet<String>();
			uview.setQid(qid);
			uview.setAlpha(alph);
			ps = connection.prepareStatement(sqlGetViewQuery);
			ps.setInt(1, qid);
			ResultSet result_view = ps.executeQuery();
			while(result_view.next())
				vqry.add(result_view.getString(1));			
			uview.setQuery(vqry);
			userviews.get(seek).add(uview);
		}

		this.viewTransformer = new ViewTransformer(k,query,alpha,distFunc,score.toString(),tagTable,tagFreqs,tag_idf, networkTable,score, connection);

		//getting the approximate statistics

		if((this.approxMethod&Methods.MET_APPR_MVAR)==Methods.MET_APPR_MVAR){
			String sqlGetDistribution = String.format(sqlGetDistributionTemplate, this.networkTable);
			ps = connection.prepareStatement(sqlGetDistribution);
			ps.setInt(1, Integer.parseInt(seeker));
			ps.setString(2, this.distFunc.toString());
			result = ps.executeQuery();
			if(result.next()){
				double mean = result.getDouble(1);
				double variance = result.getDouble(2);
				this.d_distr = new DataDistribution(mean, variance, this.number_users, query);
			}
		}

		if((this.approxMethod&Methods.MET_APPR_HIST)==Methods.MET_APPR_HIST){
			String sqlGetHistogram = String.format(sqlGetHistogramTemplate, this.networkTable);
			ps = connection.prepareStatement(sqlGetHistogram);
			ps.setInt(1, Integer.parseInt(seeker));
			ps.setString(2, this.distFunc.toString());
			result = ps.executeQuery();
			ArrayList<Integer> hist = new ArrayList<Integer>();
			while(result.next()){
				int num = result.getInt(2);
				hist.add(num);
			}
			this.d_hist = new DataHistogram(this.number_users, hist);
			for(String tag:query)
			{
				d_hist.setVals(tag, 0, 1.0f);
			}
		}
    	//this.d_distr = new DataDistribution(1.0f, 0.0f, this.number_users, query);

		Comparator comparator = new MinScoreItemComparator();   
		virtualItem = createNewCandidateItem("<rest_of_the_items>",query,virtualItem);
		candidates = new ItemList(comparator, this.score, this.number_users, k, this.virtualItem, this.d_distr, this.d_hist, this.error);  
		candidates.setContribs(high_docs);

		sqlGetDocuments = String.format(sqlGetDocumentsTemplate, this.tagTable);
		sqlGetAllDocuments = String.format(sqlGetAllDocumentsTemplate, this.tagTable);
		sqlGetTaggers = String.format(sqlGetTaggersTemplate, this.tagTable);
		idx=0;
		for(String tag:query){
			if(idx<query.size()-1){
				sqlGetDocuments+=String.format(sqlAddQueryTerm+" or ",tag);
				sqlGetAllDocuments+=String.format("\'%s\',", tag);
				sqlGetTaggers+=String.format("\'%s\',", tag);
			}
			else{
				sqlGetDocuments+=String.format(sqlAddQueryTerm+")",tag);
				sqlGetAllDocuments+=String.format("\'%s\')", tag);
				sqlGetTaggers+=String.format("\'%s\')", tag);
			}
			idx++;
		}

		total_users = 0;        
		total_lists_social = 0;
		total_documents_social = 0;
		total_documents_asocial = 0;
		total_topk_changes = 0;
		total_conforming_lists = 0;
		total_memory_seeks = 0;
		total_heap_interchanges = 0;
		total_heap_adds = 0;
		total_heap_rebuilds = 0;

		this.docs_users = new HashMap<Integer,HashMap<String,HashSet<String>>>();
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		stmt.setFetchSize(1000);
		result = stmt.executeQuery(sqlGetAllDocuments);
		while(result.next()){
			int d_usr = result.getInt(1);
			String d_itm = result.getString(2);
			String d_tag = result.getString(3);
			if(!this.docs_users.containsKey(d_usr)){
				this.docs_users.put(d_usr, new HashMap<String,HashSet<String>>());
				for(String tag:query)
					this.docs_users.get(d_usr).put(tag, new HashSet<String>());
			}
			this.docs_users.get(d_usr).get(d_tag).add(d_itm);
		}

		//    	ps = connection.prepareStatement(sqlGetTaggers);
		//    	ps.setFetchSize(1000);
		//    	result = ps.executeQuery();
		//    	while(result.next())    		
		//    		this.taggers.add(result.getString(1));
		//    	result.close();
		//    	ps.close();

		//log.info("\t\t\tread documents");


		time_preinit = System.currentTimeMillis() - time;

		//        mainLoop(k, seeker, query, maxLoops);TODO
		mainLoop(k, seeker, query);

		//arcomem uncomment to have output in xml
		//this.setQueryResultsXML(query, seeker, k, this.approxMethod, this.alpha);
		//        log.info("time_preinit{}",time_preinit);
		this.setQueryResultsArrayList(query, seeker, k, this.approxMethod, this.alpha);
		return 0;

	}*/

	/*
	 * OLD CODE NOT USEDpublic int executeQuery(String seeker, HashSet<String> query, int k, int maxLoops) throws SQLException{
	 *

		this.time_dji = 0;
		this.time_term = 0;
		this.time_clist = 0;
		this.time_queries = 0;
		this.time_heap = 0;
		this.time_dat = 0;
		this.max_pos_val = 1.0f;
		this.d_distr = null;
		this.d_hist = null;
		this.total_users = 0;
		this.total_rnd = 0;
		this.seeker = Integer.parseInt(seeker);
		vst = new ArrayList<Integer>();

		sqlGetNeighbours = String.format(sqlGetNeighboursTemplate, this.networkTable);
		log.info(sqlGetNeighbours);
		values = new ArrayList<Float>();
		taggers = new HashSet<String>();
		unknown_tf = new HashMap<String,HashSet<String>>();
		// userviews = new HashMap<String,ArrayList<UserView>>();
		for(String tag:query)
			unknown_tf.put(tag, new HashSet<String>());    	
		connection = dbConnection.DBConnect();
		connection.setAutoCommit(false);
		this.optpath.setValues(values);
		this.optpath.setDistFunc(distFunc);
		long time = System.currentTimeMillis();
		if((this.approxMethod&Methods.MET_APPR_LAND)==Methods.MET_APPR_LAND){
			landmark.setSeeker(this.seeker);
			landmark.setPathFunction(this.distFunc);
			currentUser = new UserEntry<Float>(this.seeker,1.0f);
		}
		else{
			currentUser = optpath.initiateHeapCalculation(this.seeker, query);
		}
		time_heapinit = System.currentTimeMillis() - time;
		userWeight = 1.0f;


		terminationCondition = false;

		PreparedStatement ps;
		ResultSet result;                

		//        String sqlGetNumberDocuments = String.format(sqlGetNumberDocumentsTemplate, this.tagTable);
		//        String sqlGetNumberUsers = String.format(sqlGetNumberUsersTemplate, this.tagTable);

		time = System.currentTimeMillis();
		//Establishing the global properties

		//        ps = connection.prepareStatement(sqlGetNumberDocuments);
		//    	result = ps.executeQuery();
		//    	while(result.next()){
		//    		this.number_documents = result.getInt(1);
		//    	}
		//    	ps = connection.prepareStatement(sqlGetNumberUsers);
		//    	result = ps.executeQuery();
		//    	while(result.next()){
		//    		this.number_users = result.getInt(1);
		//    	}


		//Preparing the docs list

		high_docs = new HashMap<String,Integer>();
		positions = new HashMap<String,Float>();
		userWeights = new HashMap<String,Float>();
		proximities = new ArrayList<Double>();
		tagFreqs = new HashMap<String,Integer>();
		lastpos = new HashMap<String,Integer>();
		lastval = new HashMap<String,Float>();    	    
		tag_idf = new HashMap<String,Float>();
		next_docs = new String[query.size()];
		pos = new int[query.size()];
		docs = new ResultSet[query.size()];
		int index = 0;
		for(String tag:query){
			ps = connection.prepareStatement(sqlGetDocsListByTag);
			ps.setString(1, tag);
			docs[index] = ps.executeQuery();
			log.info("Testl609");
			log.info(docs[index].toString());
			if(docs[index].next()){
				high_docs.put(tag, docs[index].getInt(2));
				next_docs[index] = docs[index].getString(1);
			}
			else{
				high_docs.put(tag, 0);
				next_docs[index] = "";
			}
			positions.put(tag, 0f);
			userWeights.put(tag, userWeight);
			pos[index]=0;
			index++;
			ps = connection.prepareStatement(sqlGetTagFrequency);
			ps.setString(1, tag);
			result = ps.executeQuery();
			int tagfreq = 0;
			if(result.next()) tagfreq = result.getInt(1);
			tagFreqs.put(tag, high_docs.get(tag));
			float tagidf = (float) Math.log(((float)this.number_documents - (float)tagfreq + 0.5)/((float)tagfreq+0.5));
			this.tag_idf.put(tag, new Float(tagidf));
		}
		proximities.add((double)userWeight);

		//getting the userviews
		String sqlGetViews = sqlGetViewsTemplate;
		int idx=0;
		for(String tag:query){
			if(idx<query.size()-1){
				sqlGetViews+=String.format("\'%s\',", tag);
			}
			else{
				sqlGetViews+=String.format("\'%s\')", tag);
			}
			idx++;
		}

		ps = connection.prepareStatement(sqlGetViews);
		ps.setString(1, distFunc.toString());
		ps.setString(2, score.toString());
		ps.setString(3, networkTable);
		ps.setDouble(4, distFunc.getCoeff());
		result = ps.executeQuery();
		while(result.next()){
			String seek = result.getString(1);
			int qid = result.getInt(2);
			double alph = result.getFloat(3);
			if(!userviews.containsKey(seek))
				userviews.put(seek, new ArrayList<UserView>());
			UserView uview = new UserView();
			HashSet<String> vqry = new HashSet<String>();
			uview.setQid(qid);
			uview.setAlpha(alph);
			ps = connection.prepareStatement(sqlGetViewQuery);
			ps.setInt(1, qid);
			ResultSet result_view = ps.executeQuery();
			while(result_view.next())
				vqry.add(result_view.getString(1));			
			uview.setQuery(vqry);
			userviews.get(seek).add(uview);
		}

		this.viewTransformer = new ViewTransformer(k,query,alpha,distFunc,score.toString(),tagTable,tagFreqs,tag_idf, networkTable,score, connection);

		//getting the approximate statistics

		if((this.approxMethod&Methods.MET_APPR_MVAR)==Methods.MET_APPR_MVAR){
			String sqlGetDistribution = String.format(sqlGetDistributionTemplate, this.networkTable);
			ps = connection.prepareStatement(sqlGetDistribution);
			ps.setInt(1, Integer.parseInt(seeker));
			ps.setString(2, this.distFunc.toString());
			result = ps.executeQuery();
			if(result.next()){
				double mean = result.getDouble(1);
				double variance = result.getDouble(2);
				this.d_distr = new DataDistribution(mean, variance, this.number_users, query);
			}
		}

		if((this.approxMethod&Methods.MET_APPR_HIST)==Methods.MET_APPR_HIST){
			String sqlGetHistogram = String.format(sqlGetHistogramTemplate, this.networkTable);
			ps = connection.prepareStatement(sqlGetHistogram);
			ps.setInt(1, Integer.parseInt(seeker));
			ps.setString(2, this.distFunc.toString());
			result = ps.executeQuery();
			ArrayList<Integer> hist = new ArrayList<Integer>();
			while(result.next()){
				int num = result.getInt(2);
				hist.add(num);
			}
			this.d_hist = new DataHistogram(this.number_users, hist);
			for(String tag:query)
			{
				d_hist.setVals(tag, 0, 1.0f);
			}
		}
    	//this.d_distr = new DataDistribution(1.0f, 0.0f, this.number_users, query);

		Comparator comparator = new MinScoreItemComparator();   
		virtualItem = createNewCandidateItem("<rest_of_the_items>",query,virtualItem);
		candidates = new ItemList(comparator, this.score, this.number_users, k, this.virtualItem, this.d_distr, this.d_hist, this.error);  
		candidates.setContribs(high_docs);


		sqlGetDocuments = String.format(sqlGetDocumentsTemplate, this.tagTable);
		sqlGetAllDocuments = String.format(sqlGetAllDocumentsTemplate, this.tagTable);
		sqlGetTaggers = String.format(sqlGetTaggersTemplate, this.tagTable);
		idx=0;
		for(String tag:query){
			if(idx<query.size()-1){
				sqlGetDocuments+=String.format(sqlAddQueryTerm+" or ",tag);
				sqlGetAllDocuments+=String.format("\'%s\',", tag);
				sqlGetTaggers+=String.format("\'%s\',", tag);
			}
			else{
				sqlGetDocuments+=String.format(sqlAddQueryTerm+")",tag);
				sqlGetAllDocuments+=String.format("\'%s\')", tag);
				sqlGetTaggers+=String.format("\'%s\')", tag);
			}
			idx++;
		}

		total_users = 0;        
		total_lists_social = 0;
		total_documents_social = 0;
		total_documents_asocial = 0;
		total_topk_changes = 0;
		total_conforming_lists = 0;
		total_memory_seeks = 0;
		total_heap_interchanges = 0;
		total_heap_adds = 0;
		total_heap_rebuilds = 0;

		this.docs_users = new HashMap<Integer,HashMap<String,HashSet<String>>>();
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		stmt.setFetchSize(1000);
		result = stmt.executeQuery(sqlGetAllDocuments);
		while(result.next()){
			int d_usr = result.getInt(1);
			String d_itm = result.getString(2);
			String d_tag = result.getString(3);
			if(!this.docs_users.containsKey(d_usr)){
				this.docs_users.put(d_usr, new HashMap<String,HashSet<String>>());
				for(String tag:query)
					this.docs_users.get(d_usr).put(tag, new HashSet<String>());
				//    				System.out.println("docs"+docs_users);//TODO
			}
			this.docs_users.get(d_usr).get(d_tag).add(d_itm);
		}

		//    	ps = connection.prepareStatement(sqlGetTaggers);
		//    	ps.setFetchSize(1000);
		//    	result = ps.executeQuery();
		//    	while(result.next())    		
		//    		this.taggers.add(result.getString(1));
		//    	result.close();
		//    	ps.close();

		//log.info("\t\t\tread documents");


		time_preinit = System.currentTimeMillis() - time;

		//        mainLoop(k, seeker, query, maxLoops);TODO
		mainLoop(k, seeker, query);


		//arcomem uncomment to have output in xml
		//this.setQueryResultsXML(query, seeker, k, this.approxMethod, this.alpha);
		//        log.info("time_preinit{}",time_preinit);
		this.setQueryResultsArrayList(query, seeker, k, this.approxMethod, this.alpha);

		return 0;
	}*/

	/**
	 * Main call from TopKAlgorithm class, call this after building a new object to run algorithm
	 * l 783- 1035
	 */
	public int executeQuery(String seeker, HashSet<String> query, int k) throws SQLException{
		System.out.println("784");
		this.time_dji = 0;
		this.time_term = 0;
		this.time_clist = 0;
		this.time_queries = 0;
		this.time_heap = 0;
		this.time_dat = 0;
		this.max_pos_val = 1.0f;
		this.d_distr = null;
		this.d_hist = null;
		this.total_users = 0;
		this.total_rnd = 0;
		this.seeker = Integer.parseInt(seeker);
		vst = new ArrayList<Integer>();

		//sqlGetNeighbours = String.format(sqlGetNeighboursTemplate, this.networkTable);
		values = new ArrayList<Float>();
		taggers = new HashSet<String>();
		unknown_tf = new HashMap<String,HashSet<String>>();
		// userviews = new HashMap<String,ArrayList<UserView>>();
		for(String tag:query)
			unknown_tf.put(tag, new HashSet<String>());    	
		connection = dbConnection.DBConnect();
		connection.setAutoCommit(false);
		this.optpath.setValues(values);
		this.optpath.setDistFunc(distFunc);
		//long time = System.currentTimeMillis();
		if((this.approxMethod&Methods.MET_APPR_LAND)==Methods.MET_APPR_LAND){
			landmark.setSeeker(this.seeker);
			landmark.setPathFunction(this.distFunc);
			currentUser = new UserEntry<Float>(this.seeker,1.0f);
		}
		else{
			currentUser = optpath.initiateHeapCalculation(this.seeker, query);
		}
		//time_heapinit = System.currentTimeMillis() - time;
		userWeight = 1.0f;

		terminationCondition = false;
		PreparedStatement ps;
		ResultSet result;                

		//        String sqlGetNumberDocuments = String.format(sqlGetNumberDocumentsTemplate, this.tagTable);
		//        String sqlGetNumberUsers = String.format(sqlGetNumberUsersTemplate, this.tagTable);

		//time = System.currentTimeMillis();
		//Establishing the global properties

		//        ps = connection.prepareStatement(sqlGetNumberDocuments);
		//    	result = ps.executeQuery();
		//    	while(result.next()){
		//    		this.number_documents = result.getInt(1);
		//    	}
		//    	ps = connection.prepareStatement(sqlGetNumberUsers);
		//    	result = ps.executeQuery();
		//    	while(result.next()){
		//    		this.number_users = result.getInt(1);
		//    	}

		//Preparing the docs list
		high_docs = new HashMap<String,Integer>();
		positions = new HashMap<String,Float>();
		userWeights = new HashMap<String,Float>();
		proximities = new ArrayList<Double>();
		tagFreqs = new HashMap<String,Integer>();
		lastpos = new HashMap<String,Integer>();
		lastval = new HashMap<String,Float>();    	    
		tag_idf = new HashMap<String,Float>();
		next_docs = new String[query.size()];
		pos = new int[query.size()];
		docs = new ResultSet[query.size()];
		int index = 0;
		for(String tag:query){
			ps = connection.prepareStatement(sqlGetDocsListByTag);
			ps.setString(1, tag);
			docs[index] = ps.executeQuery();
			if(docs[index].next()){
				int getInt2 = docs[index].getInt(2);
				String getString1 = docs[index].getString(1);
				high_docs.put(tag, getInt2);
				next_docs[index] = getString1;
			}
			else{
				high_docs.put(tag, 0);
				next_docs[index] = "";
			}
			positions.put(tag, 0f);
			userWeights.put(tag, userWeight);
			pos[index]=0;
			index++;
			ps = connection.prepareStatement(sqlGetTagFrequency);
			ps.setString(1, tag);
			result = ps.executeQuery();
			int tagfreq = 0;
			if(result.next()) tagfreq = result.getInt(1);
			tagFreqs.put(tag, high_docs.get(tag));
			float tagidf = (float) Math.log(((float)this.number_documents - (float)tagfreq + 0.5)/((float)tagfreq+0.5));
			this.tag_idf.put(tag, new Float(tagidf));
		}
		proximities.add((double)userWeight);

		//getting the userviews
		String sqlGetViews = sqlGetViewsTemplate;
		int idx=0;
		for(String tag:query){
			if(idx<query.size()-1){
				sqlGetViews+=String.format("\'%s\',", tag);
			}
			else{
				sqlGetViews+=String.format("\'%s\')", tag);
			}
			idx++;
		}

		ps = connection.prepareStatement(sqlGetViews);
		ps.setString(1, distFunc.toString());
		ps.setString(2, score.toString());
		ps.setString(3, networkTable);
		ps.setDouble(4, distFunc.getCoeff());
		result = ps.executeQuery();
		while(result.next()){
			String seek = result.getString(1);
			int qid = result.getInt(2);
			double alph = result.getFloat(3);
			if(!userviews.containsKey(seek))
				userviews.put(seek, new ArrayList<UserView>());
			UserView uview = new UserView();
			HashSet<String> vqry = new HashSet<String>();
			uview.setQid(qid);
			uview.setAlpha(alph);
			ps = connection.prepareStatement(sqlGetViewQuery);
			ps.setInt(1, qid);
			ResultSet result_view = ps.executeQuery();
			while(result_view.next())
				vqry.add(result_view.getString(1));			
			uview.setQuery(vqry);
			userviews.get(seek).add(uview);
		}

		this.viewTransformer = new ViewTransformer(k,query,alpha,distFunc,score.toString(),tagTable,tagFreqs,tag_idf, networkTable,score, connection);

		//getting the approximate statistics

		if((this.approxMethod&Methods.MET_APPR_MVAR)==Methods.MET_APPR_MVAR){
			String sqlGetDistribution = String.format(sqlGetDistributionTemplate, this.networkTable);
			ps = connection.prepareStatement(sqlGetDistribution);
			ps.setInt(1, Integer.parseInt(seeker));
			ps.setString(2, this.distFunc.toString());
			result = ps.executeQuery();
			if(result.next()){
				double mean = result.getDouble(1);
				double variance = result.getDouble(2);
				this.d_distr = new DataDistribution(mean, variance, this.number_users, query);
			}
		}

		if((this.approxMethod&Methods.MET_APPR_HIST)==Methods.MET_APPR_HIST){
			String sqlGetHistogram = String.format(sqlGetHistogramTemplate, this.networkTable);
			ps = connection.prepareStatement(sqlGetHistogram);
			ps.setInt(1, Integer.parseInt(seeker));
			ps.setString(2, this.distFunc.toString());
			result = ps.executeQuery();
			ArrayList<Integer> hist = new ArrayList<Integer>();
			while(result.next()){
				int num = result.getInt(2);
				hist.add(num);
			}
			this.d_hist = new DataHistogram(this.number_users, hist);
			for(String tag:query)
			{
				d_hist.setVals(tag, 0, 1.0f);
			}
		}
    	//this.d_distr = new DataDistribution(1.0f, 0.0f, this.number_users, query);

		Comparator comparator = new MinScoreItemComparator();   
		virtualItem = createNewCandidateItem("<rest_of_the_items>",query,virtualItem);
		candidates = new ItemList(comparator, this.score, this.number_users, k, this.virtualItem, this.d_distr, this.d_hist, this.error);  
		candidates.setContribs(high_docs);

		sqlGetDocuments = String.format(sqlGetDocumentsTemplate, this.tagTable);
		sqlGetAllDocuments = String.format(sqlGetAllDocumentsTemplate, this.tagTable);
		sqlGetTaggers = String.format(sqlGetTaggersTemplate, this.tagTable);
		idx=0;
		for(String tag:query){
			if(idx<query.size()-1){
				sqlGetDocuments+=String.format(sqlAddQueryTerm+" or ",tag);
				sqlGetAllDocuments+=String.format("\'%s\',", tag);
				sqlGetTaggers+=String.format("\'%s\',", tag);
			}
			else{
				sqlGetDocuments+=String.format(sqlAddQueryTerm+")",tag);
				sqlGetAllDocuments+=String.format("\'%s\')", tag);
				sqlGetTaggers+=String.format("\'%s\')", tag);
			}
			idx++;
		}

		total_users = 0;        
		total_lists_social = 0;
		total_documents_social = 0;
		total_documents_asocial = 0;
		total_topk_changes = 0;
		total_conforming_lists = 0;
		total_memory_seeks = 0;
		total_heap_interchanges = 0;
		total_heap_adds = 0;
		total_heap_rebuilds = 0;

		this.docs_users = new HashMap<Integer,HashMap<String,HashSet<String>>>();
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		stmt.setFetchSize(1000);
		result = stmt.executeQuery(sqlGetAllDocuments);
		while(result.next()){
			int d_usr = result.getInt(1);
			String d_itm = result.getString(2);
			String d_tag = result.getString(3);
			if(!this.docs_users.containsKey(d_usr)){
				this.docs_users.put(d_usr, new HashMap<String,HashSet<String>>());
				for(String tag:query)
					this.docs_users.get(d_usr).put(tag, new HashSet<String>());
				//    				System.out.println("docs"+docs_users);//TODO
			}
			this.docs_users.get(d_usr).get(d_tag).add(d_itm);
		}

		//    	ps = connection.prepareStatement(sqlGetTaggers);
		//    	ps.setFetchSize(1000);
		//    	result = ps.executeQuery();
		//    	while(result.next())    		
		//    		this.taggers.add(result.getString(1));
		//    	result.close();
		//    	ps.close();

		//log.info("\t\t\tread documents");

		//time_preinit = System.currentTimeMillis() - time;

		mainLoop(k, seeker, query); /* MAIN ALGORITHM */

		//arcomem uncomment to have output in xml
		//this.setQueryResultsXML(query, seeker, k, this.approxMethod, this.alpha);
		//        log.info("time_preinit{}",time_preinit);
		this.setQueryResultsArrayList(query, seeker, k, this.approxMethod, this.alpha);

		return 0;
	}

	/*
	 * MAIN LOOP
	 */
	protected void mainLoop(int k, String seeker, HashSet<String> query) throws SQLException{
		int loops=0; //amine
		int skipped_tests = 10000;
		int steps = 1;
		//long time = System.currentTimeMillis();
		firstPossible = true;
		needUnseen = true;
		skipViews = false;
		guaranteed = new HashSet<String>();
		possible = new HashSet<String>();
		do{
			docs_inserted = false;
			boolean social = false;
			finished = true;
			boolean socialBranch = chooseBranch(query);      			
			if(socialBranch){
				processSocial(query);
				if(((this.approxMethod&Methods.MET_VIEW)==Methods.MET_VIEW)&&userviews.containsKey(currentUser.getEntryId())){
					boolean exist = viewTransformer.computeUsingViews(userWeight, userviews.get(currentUser.getEntryId()));
					//        				HashMap<String,ViewScore> view = (HashMap<String,ViewScore>) view_ws.clone();
					////        				if((this.approxMethod&Methods.MET_VIEW_TOPK)==0){ //only compute bestscores when we can't say anything
					//        				HashMap<String,ViewScore> view_bs = viewTransformer.getTransformedView(Integer.parseInt(currentUser.getEntryId()),false);
					//        				for(String it:view_bs.keySet())
					//        					if(!view.containsKey(it)) view.put(it, view_bs.get(it));
					////        				}
					candidates.setViews(true);
					if(exist){        					
						processView(query);        				         				            			
					}
				}
				social=true;
				if((approxMethod&Methods.MET_TOPKS)==Methods.MET_TOPKS) {
					lookIntoList(query);   //the "peek at list" procedure
				}
			}
			else {
				processTextual(query);
				log.info("textual branch");
			}
			if(social) this.total_lists_social++;

			//long time_1 = System.currentTimeMillis();
			steps = (steps+1)%skipped_tests;
			if((steps==0)||(!needUnseen&&((approxMethod&Methods.MET_ET)==Methods.MET_ET))){
				try {
					/*
					 * During the terminationCondition method, look up at top_items of different ILs, we add
					 * them if necessary to the top-k answer of the algorithm.
					 */
					terminationCondition = candidates.terminationCondition(userWeight, k, query.size(), alpha, this.number_users, tag_idf, high_docs, total_sum, userWeights, positions, approxMethod, docs_inserted, needUnseen, guaranteed, possible);
				} catch (IOException e) {
					e.printStackTrace();
				}
				////        		if((this.approxMethod&Methods.MET_VIEW)==Methods.MET_VIEW){
				//        			if((terminationCondition==false)&&(candidates.getContribItem()!=null))
				//        				getAllItemScores(candidates.getContribItem(),query);
				////        		}
				//For statistics only
				if(candidates.topkChange()){
					this.total_topk_changes++;
					for(String tag:query){
						this.lastpos.put(tag, positions.get(tag).intValue());
						this.lastval.put(tag, userWeights.get(tag));
					}
				}
				candidates.resetChange();
			}
			else{
				terminationCondition=false;
			}
			//long time_2 = System.currentTimeMillis();
			//this.time_term+=(time_2 - time_1);

			loops++;
		}while(!terminationCondition&&!finished);
		this.numloops=loops;
		System.out.println("loops="+loops);
		//time_loop = System.currentTimeMillis() - time;
	}

	protected boolean chooseBranch(HashSet<String> query){
		double upper_social_score;
		double upper_docs_score;
		boolean textual = false;
		for(String tag:query){
			if((approxMethod&Methods.MET_TOPKS)==Methods.MET_TOPKS)
				//    			upper_social_score = (1-alpha)*userWeights.get(tag)*high_docs.get(tag);
				upper_social_score = (1-alpha)*userWeights.get(tag)*candidates.getSocialContrib(tag);
			else
				//    			/*if((approxMethod&Methods.MET_EX_OPT)==Methods.MET_EX_OPT)
				//    		upper_social_score = (1-alpha)*userWeights.get(tag)*candidates.getMaxContrib(tag, high_docs.get(tag));
				//    	else*/
				upper_social_score = (1-alpha)*userWeights.get(tag)*tagFreqs.get(tag);
			if((approxMethod&Methods.MET_TOPKS)==Methods.MET_TOPKS)
				upper_docs_score = alpha*candidates.getNormalContrib(tag);
			else
				upper_docs_score = alpha*high_docs.get(tag);
			if(!((upper_social_score==0)&&(upper_docs_score==0))) finished = false;
			if((upper_social_score!=0)||(upper_docs_score!=0)) textual = textual || (upper_social_score<=upper_docs_score);
		}
		return !textual;

	}

	/*
	 * Social process of the TOPKS algorithm
	 */
	protected void processSocial(HashSet<String> query) throws SQLException{
		HashMap<String, HashSet<String>> soclist = new HashMap<String, HashSet<String>>();
		PreparedStatement ps;
		ResultSet result;
		int currentUserId;
		long time_1;
		long time_2;
		int index = 0;

		if(currentUser!=null) vst.add(currentUser.getEntryId());

		/*
		 * for all tags in the query Q, triples Tagged(u,i,t_j)
		 */
		for(String tag:query){  		    		
			if(currentUser!=null){
				boolean found_docs = false;
				pos[index]++;   			
				float prev_part_sum = pos[index];
				positions.put(tag, prev_part_sum);
				if((approxMethod&Methods.MET_APPR_MVAR)==Methods.MET_APPR_MVAR)
					d_distr.setPos(tag, userWeight, pos[index]+1);
				else if((approxMethod&Methods.MET_APPR_HIST)==Methods.MET_APPR_HIST)
					d_hist.setVals(tag, pos[index]+1, userWeight);
				if((this.approxMethod&Methods.MET_APPR_LAND)==Methods.MET_APPR_LAND){
					userWeights.put(tag, landmark.getMaxRemaining());
				}
				else{
					userWeights.put(tag, userWeight);
				}
				currentUserId = currentUser.getEntryId();
				//    			if(taggers.contains(currentUserId)){
				//    				if(soclist.size()==0){    					
				//    					for(String t1:query)
				//    						soclist.put(t1, new HashSet<String>());
				//    					if(this.taggers.contains(currentUserId)){
				//    						time_1 = System.currentTimeMillis();
				//    						ps = connection.prepareStatement(sqlGetDocuments);
				//    						ps.setInt(1, Integer.parseInt(currentUserId));
				//    						//ps.setString(2, tag);
				//    						ps.setFetchSize(1000);
				//    						result = ps.executeQuery();
				//    						time_2 = System.currentTimeMillis();
				//    						this.time_queries+=(time_2 - time_1);    				
				//    						while(result.next()){
				//    							String tag_id = result.getString(1);
				//    							String item_id = result.getString(2);
				//    							soclist.get(tag_id).add(item_id);
				//    						}    				
				//    					}    				
				//    				}
				if(this.docs_users.containsKey(currentUserId) && !(currentUserId==seeker)){
					if(docs_users.get(currentUserId).containsKey(tag)) {
						for(String itemId:docs_users.get(currentUserId).get(tag)){
							found_docs = true;
							//time_1 = System.currentTimeMillis();
							Item<String> item = candidates.findItem(itemId);
							float userW = 0;
							if(item==null){
								item = createNewCandidateItem(itemId, query,item); 
								item.setMaxScorefromviews(bestScoreEstim);
								for(String tag1:query)
									if(!item.tdf.containsKey(tag1)) {
										unknown_tf.get(tag1).add(itemId);
									}
								// candidates.addItem(item);
							}  
							else
								candidates.removeItem(item);
							userW = userWeight;    					
							item.updateScore(tag, userW, pos[index], approxMethod);
							// item.computeBestScore(high_docs, total_sum, userWeights, positions, approxMethod);
							candidates.addItem(item);


							//time_2 = System.currentTimeMillis();
							//this.time_queries+=(time_2 - time_1);
							docs_inserted = true;
							total_documents_social++;                            
						}
					}
				}
				if(found_docs){
					total_conforming_lists++;
					docs_inserted = true;
				}
			}
			else{
				currentUserId = Integer.MAX_VALUE;
				pos[index]++;
				userWeight = 0;
				float prev_part_sum = pos[index];
				positions.put(tag, prev_part_sum);
				positions.put(tag, prev_part_sum);
				if((approxMethod&Methods.MET_APPR_MVAR)==Methods.MET_APPR_MVAR)
					d_distr.setPos(tag, userWeight, pos[index]+1);
				else if((approxMethod&Methods.MET_APPR_HIST)==Methods.MET_APPR_HIST)
					d_hist.setVals(tag, pos[index]+1, userWeight);
				userWeights.put(tag, userWeight);
			}
			index++;
		}/* END FOR ALL TAGS IN QUERY Q */

		//time_1 = System.currentTimeMillis();
		if((this.approxMethod&Methods.MET_APPR_LAND)==Methods.MET_APPR_LAND){
			currentUser = landmark.getNextUser();
		}
		else{
			currentUser = optpath.advanceFriendsList(currentUser, query);
		}
		//time_2 = System.currentTimeMillis();
		//this.time_heap+=(time_2 - time_1);
		if(currentUser!=null)
			userWeight = currentUser.getDist().floatValue();
		else
			userWeight = 0.0f;
		proximities.add((double)userWeight);

	}

	private void lookIntoList(HashSet<String> query){
		int index=0;
		boolean found = true;
		String[] tags = new String[query.size()];
		for(String tag:query){
			tags[index] = tag;
			index++;
		}
		System.out.println(next_docs[0]+"");//toString());
		while(found){
			for(String tag:query){
				found = false; // ?????
				for(index=0;index<query.size();index++) {
					if(unknown_tf.get(tag).contains(next_docs[index])){
						Item<String> item1 = candidates.findItem(next_docs[index]);
						//log.info("\t item {} has been found in the list",item.getItemId());
						candidates.removeItem(item1);
						item1.updateScoreDocs(tags[index], high_docs.get(tags[index]),approxMethod);
						unknown_tf.get(tags[index]).remove(next_docs[index]); 
						advanceTextualList(tags[index],index);
						//    					item1.computeBestScore(high_docs, total_sum, userWeights, positions, approxMethod);
						candidates.addItem(item1);
						found = true;
					}
				}
			}
		}
	}

	protected void processTextual(HashSet<String> query) throws SQLException{
		int index = 0;
		for(String tag:query){
			if(next_docs[index]!=""){
				Item<String> item = candidates.findItem(next_docs[index]);
				if(item==null)
					item = createNewCandidateItem(next_docs[index], query, item);
				//    			candidates.addItem(item);
				//    		}
				else
					candidates.removeItem(item);
				item.updateScoreDocs(tag, high_docs.get(tag),approxMethod);
				if(unknown_tf.get(tag).contains(item.getItemId())) unknown_tf.get(tag).remove(item.getItemId());
				//    			item.computeBestScore(high_docs, total_sum, userWeights, positions, approxMethod);
				candidates.addItem(item);
				docs_inserted = true;                    		
				advanceTextualList(tag,index);
			}
			index++;
		}
	}

	/*
	 * Process with views
	 */
	protected void processView(HashSet<String> query) throws SQLException{
		HashMap<String,ViewScore> guar = viewTransformer.getGuaranteed();
		HashMap<String,ViewScore> need = viewTransformer.getPossible();
		boolean early = viewTransformer.isEarly();
		needUnseen = needUnseen && !early;
		for(String itm:guar.keySet()){
			Item<String> itm_c = candidates.findItem(itm);
			if(itm_c==null){
				itm_c = createNewCandidateItem(itm, query, itm_c);
			}
			else
				candidates.removeItem(itm_c);
			itm_c.setMinScorefromviews(guar.get(itm).getWscore());
			itm_c.setMaxScorefromviews(guar.get(itm).getBscore());    		
			candidates.addItem(itm_c);
			guaranteed.add(itm);
		}
		if(early){
			HashSet<String> new_need = new HashSet<String>();
			for(String itm:need.keySet()){
				Item<String> itm_c = candidates.findItem(itm);
				if(itm_c==null){
					itm_c = createNewCandidateItem(itm, query, itm_c);
				}
				else
					candidates.removeItem(itm_c);
				itm_c.setMinScorefromviews(need.get(itm).getWscore());
				itm_c.setMaxScorefromviews(need.get(itm).getBscore());    		
				candidates.addItem(itm_c);
				boolean add = firstPossible || possible.contains(itm);
				if(add)
					new_need.add(itm);
			}

			possible = new_need;
		}
		if(possible.size()>0)
			firstPossible = false;
	}

	protected void advanceTextualList(String tag, int index){

		try {
			if(docs[index].next()){
				total_documents_asocial++;
				high_docs.put(tag, docs[index].getInt(2));
				next_docs[index] = docs[index].getString(1);
			}
			else{
				high_docs.put(tag, 0);
				next_docs[index] = "";
				//opportunity-=alpha;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	protected void getAllItemScores(String item, HashSet<String> query) throws SQLException{
		Item<String> itm = candidates.findItem(item);
		for(String tag:query)
			if(!itm.tdf.containsKey(tag)){
				PreparedStatement stmt = connection.prepareStatement(sqlGetDocumentTf);
				stmt.setString(1, item);
				stmt.setString(2, tag);
				ResultSet result = stmt.executeQuery();
				int tf = 0;
				if(result.next())
					tf = result.getInt(1);
				result.close();
				stmt.close();
				itm.updateScoreDocs(tag, tf, approxMethod);
				this.total_rnd++;
			}
		candidates.removeItem(itm);
		candidates.addItem(itm);
	}

	protected Item<String> createNewCandidateItem(String itemId, HashSet<String> tagList, Item<String> item) throws SQLException{
		item = new Item<String>(itemId, this.alpha, this.number_users, this.score,  this.d_distr, this.d_hist, this.error);        
		for(String tag:tagList){
			item.addTag(tag, tag_idf.get(tag));        
			unknown_tf.get(tag).add(itemId);			
		}
		return item;
	}

	public String statistics(){
		String idfs="";
		String tkpos="";
		String tkval="";
		for(String tag:this.tag_idf.keySet()){
			idfs = String.format(Locale.US,"%.3f", tag_idf.get(tag));
			tkpos = String.format(Locale.US,"%d", lastpos.get(tag));
			tkval = String.format(Locale.US,"%.3f", lastval.get(tag));
		}
		return String.format(Locale.US, ""+
				"<br><stat><b>Time</b>: main loop <b>%.3f</b> sec</stat><br><br>"+
				//    			"Time for queries: %.3fsec Time for cand list: %.3fsec Time for termination condition: %.3fsec<br>"+
				//    			"Time for heap transversal: %.3fsec Dji alg.: %.3f data ret: %.3f relaxation: %.3f<br>"+
				//    			"Heap extractions: %d<br>"+
				//    			"Heap insertions: %d<br>"+
				//    			"Heap rebuilds:%d<br>"+
				//    			"Heap interchanges: %d<br>"+
				//    			"Total memory seeks: %d<br>"+
				"<stat><b>%d</b> total <b>user lists</b>, last proximity <b>%.3f</b></stat><br><br>"+
				"<stat><b>%d top-k changes</b>, last at position <b>%s</b></stat><br><br>"+
				"<stat><b>%d</b> docs in <b>user lists</b>, <b>%d</b> in <b>inverted lists</b>, <b>%d</b> random</stat><br><br>",
				//    			(float)time_preinit/(float)1000, 
				(float)time_loop/(float)1000,
				total_lists_social, this.userWeight,
				total_topk_changes, tkpos,
				total_documents_social, total_documents_asocial, total_rnd);
		//    			(float)time_heapinit/(float)1000, (float)time_preinit/(float)1000, (float)time_loop/(float)1000,
		//    			(float)time_queries/(float)1000, (float)time_clist/(float)1000, (float)time_term/(float)1000, 
		//    			(float)time_heap/(float)1000, (float)time_dji/(float)1000, (float)time_dat/(float)1000, (float)time_rel/(float)1000,
		//    			total_users, total_heap_adds, total_heap_interchanges, total_memory_seeks, 
		//    			total_documents_social, total_conforming_lists, total_lists_social, 
		//    			total_documents_asocial, total_topk_changes, 
		//    			tkpos, tkval, idfs);
	}

	//8/9/14

	//    protected void setQueryResultsXML(HashSet<String> query, String seeker, int k, int method, float alpha){
	//    	String queryStr="";
	//    	for(String tag:query) queryStr+=(tag+" ");
	//    	int lastp = 0;
	//    	int totp = 0;
	//    	float lastv = 0;
	//    	for(String tag:this.tag_idf.keySet()){
	//    		if(lastpos.containsKey(tag)){
	//    			totp+=lastpos.get(tag);
	//    			if(lastp<lastpos.get(tag)){
	//    				lastp=lastpos.get(tag);
	//    				lastv=lastval.get(tag);
	//    			}
	//    		}
	//    	}
	//    	totp = totp/query.size();
	////    	this.newXMLResults = String.format(Locale.US,"<query network=\"%s\" func=\"%s\" tags=\"%s\" seeker=\"%s\" k=\"%d\" method=\"%d\" alpha=\"%.2f\">", this.networkTable, this.distFunc.toString(), queryStr, seeker, k, method, alpha);
	//    	
	////    	this.newXMLResults = String.format(Locale.US, "<ResultSet seeker=>\"%s\"", seeker);
	//    	
	//    	int position=0;
	//    	for(String itid:this.candidates.get_topk()){
	//    		Item<String> item = candidates.findItem(itid);
	//    		this.newXMLResults+=String.format(Locale.US,"<result score=\"%.5f\">%s</result>", item.getComputedScore(), protectSpecialCharacters(item.getItemId()));
	//    		position++;
	//    	}
	//    	
	//    	this.newXMLResults+="</ResultSet>\n";
	//    	
	//    	this.newXMLResults+="<stats>";
	//    	this.newXMLResults+=String.format(Locale.US,"<time>%.3f</time>", (float)(time_loop)/(float)1000);
	//    	this.newXMLResults+=String.format("<pos_last_topkchg>%d</pos_last_topkchg>", lastp);
	//    	this.newXMLResults+=String.format(Locale.US,"<val_last_topkchg>%.5f</val_last_topkchg>", lastv);
	//    	this.newXMLResults+=String.format("<pos_last>%d</pos_last>", total_lists_social);
	//    	this.newXMLResults+=String.format(Locale.US,"<val_last>%.5f</val_last>", this.userWeight);
	//    	//this.resultsXML+=String.format("<maxheap>%d</maxheap>", this.prioQueue.getMaxsize());
	//    	this.newXMLResults+=String.format("<social_docs>%d</social_docs>", total_documents_social);
	//    	this.newXMLResults+=String.format("<social_queries>%d</social_queries>", total_lists_social);
	//    	this.newXMLResults+=String.format("<normal_docs>%d</normal_docs>", total_documents_asocial);
	//    	this.newXMLResults+="</stats>\n";
	//    	
	////    	this.newXMLResults+="</query>";
	//    		
	//    	try {
	//    		Calendar cal = Calendar.getInstance();
	//    		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
	//    		FileWriter fileXML = new FileWriter(pathToQueries+"query_"+sdf.format(cal.getTime())+".xml");
	//    		fileXML.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
	//    		fileXML.write(newXMLResults);
	//    		fileXML.close();
	//
	//    		FileWriter fileCSV = new FileWriter(pathToDistributions+"dist_"+seeker+"_"+this.networkTable+"_"+this.distFunc.toString()+".csv");
	//    		for(float val:values)
	//    			fileCSV.write(String.format(Locale.US,"%.5f\t", val));
	//    		fileCSV.close();
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		} catch (Exception ex) {
	//			ex.printStackTrace();
	//		}
	//    	
	//    }


	protected void setQueryResultsArrayList(HashSet<String> query, String seeker, int k, int method, float alpha){
		//    	log.info("query res");
		System.out.println("this.candidates.get_topk().size()="+this.candidates.get_topk().size());
		String queryStr="";
		//item 
		String singItem = "";
		for(String tag:query) queryStr+=(tag+" ");
		int lastp = 0;
		int totp = 0;
		float lastv = 0;
		for(String tag:this.tag_idf.keySet()){
			if(lastpos.containsKey(tag)){
				totp+=lastpos.get(tag);
				if(lastp<lastpos.get(tag)){
					lastp=lastpos.get(tag);
					lastv=lastval.get(tag);
				}
			}
		}
		totp = totp/query.size();

		//    	this.newXMLResults = String.format(Locale.US,"<query network=\"%s\" func=\"%s\" tags=\"%s\" seeker=\"%s\" k=\"%d\" method=\"%d\" alpha=\"%.2f\">", this.networkTable, this.distFunc.toString(), queryStr, seeker, k, method, alpha);
		//    	this.newXMLResults = String.format(Locale.US, "<ResultSet seeker=\"%s\">", seeker);

		String str="";
		this.newXMLResults+= "\n<TopkResults>\n";

		int position=0;


		for(String itid:this.candidates.get_topk()){
			Item<String> item = candidates.findItem(itid);
			str=protectSpecialCharacters(item.getItemId());
			//    		log.info("{}",protectSpecialCharacters(item.getItemId()));
			this.resultList.addResult(str, item.getComputedScore(), item.getBestscore());
			//    		System.out.println(item.getComputedScore()+"|"+item.getBestscore());
			resultList.setNbLoops(this.numloops);//amine populate resultList object

			//    		int n=this.resultList.getResult().size();
			//    		log.info("{}",this.resultList.getResult().get(n-1));

			//    		this.resultsXML+=String.format(Locale.US,"<result score=\"%.5f\">%s</result>", item.getComputedScore(), protectSpecialCharacters(item.getItemId()));
			this.newXMLResults+=String.format(Locale.US,"<result  minscore=\"%.5f\" maxscore=\"%.5f\">%s</result>",
					item.getComputedScore(), item.getBestscore(), protectSpecialCharacters(item.getItemId()));

			this.newXMLResults+="\n";
			position++;


		}

		this.newXMLResults+="</TopkResults>\n";

		//amine add bucket component
		this.newBucketResults+="<BucketResults>\n";

		Iterator<Entry<String, Item<String>>> iter=  this.candidates.getItems().entrySet().iterator();

		while(iter.hasNext()){
			String currItem=iter.next().getKey();
			if(! this.candidates.get_topk().contains(currItem)){ //proceed with items not in the topks only
				Item<String> item = candidates.findItem(currItem);
				str=protectSpecialCharacters(item.getItemId());
				this.resultList.addResult(str, item.getComputedScore(), item.getBestscore());
				resultList.setNbLoops(this.numloops);
				// curr_item.computeBestScore(high, total_sum, user_weights, positions, approx);

				this.newBucketResults+=String.format(Locale.US,"<bucket  minscore=\"%.5f\" maxscore=\"%.5f\">%s</bucket>",
													 item.getComputedScore(), item.getBestscore(), protectSpecialCharacters(item.getItemId()));
				this.newBucketResults+="\n";
			}
		}

		this.newBucketResults+="</BucketResults>\n";
		//        	System.out.println(candidates.getMax_from_rest());
		this.newBucketResults+=String.format(Locale.US,"<Unseen  Max_from_rest=\"%.5f\"/>\n",
				candidates.getMax_from_rest());

		this.newXMLStats+="<stats>";
		this.newXMLStats+=String.format(Locale.US,"<time>%.3f</time>", (float)(time_loop)/(float)1000);
		this.newXMLStats+=String.format("<pos_last_topkchg>%d</pos_last_topkchg>", lastp);
		this.newXMLStats+=String.format(Locale.US,"<val_last_topkchg>%.5f</val_last_topkchg>", lastv);
		this.newXMLStats+=String.format("<pos_last>%d</pos_last>", total_lists_social);
		this.newXMLStats+=String.format(Locale.US,"<val_last>%.5f</val_last>", this.userWeight);
		//this.resultsXML+=String.format("<maxheap>%d</maxheap>", this.prioQueue.getMaxsize());
		this.newXMLStats+=String.format("<social_docs>%d</social_docs>", total_documents_social);
		this.newXMLStats+=String.format("<social_queries>%d</social_queries>", total_lists_social);
		this.newXMLStats+=String.format("<normal_docs>%d</normal_docs>", total_documents_asocial);
		this.newXMLStats+="</stats>";

		//   	this.newXMLResults+="</ResultSet>";

		//    	try {
		//    		Calendar cal = Calendar.getInstance();
		//    		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
		//    		FileWriter fileXML = new FileWriter(pathToQueries+"query_"+sdf.format(cal.getTime())+".xml");
		//    		fileXML.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
		//    		fileXML.write(resultsXML);
		//    		fileXML.close();
		//
		//    		FileWriter fileCSV = new FileWriter(pathToDistributions+"dist_"+seeker+"_"+this.networkTable+"_"+this.distFunc.toString()+".csv");
		//    		for(float val:values)
		//    			fileCSV.write(String.format(Locale.US,"%.5f\t", val));
		//    		fileCSV.close();
		//		} catch (IOException e) {
		//			e.printStackTrace();
		//		} catch (Exception ex) {
		//			ex.printStackTrace();
		//		}

	}
	public void setLandmarkPaths(LandmarkPathsComputing landmark){
		this.landmark = landmark;
	}

	private String protectSpecialCharacters(String originalUnprotectedString) {
		if (originalUnprotectedString == null) {
			return null;
		}
		boolean anyCharactersProtected = false;

		StringBuffer stringBuffer = new StringBuffer();
		for (int i = 0; i < originalUnprotectedString.length(); i++) {
			char ch = originalUnprotectedString.charAt(i);

			boolean controlCharacter = ch < 32;
			boolean unicodeButNotAscii = ch > 126;
			boolean characterWithSpecialMeaningInXML = ch == '<' || ch == '&' || ch == '>';

			if (characterWithSpecialMeaningInXML || unicodeButNotAscii || controlCharacter) {
				stringBuffer.append("&#" + (int) ch + ";");
				anyCharactersProtected = true;
			} else {
				stringBuffer.append(ch);
			}
		}
		if (anyCharactersProtected == false) {
			return originalUnprotectedString;
		}

		return stringBuffer.toString();
	}

	public TreeSet<Item<String>> getResults(){
		TreeSet<Item<String>> results = new TreeSet<Item<String>>();
		for(String itid:candidates.get_topk())
			results.add(candidates.findItem(itid));
		return results;
	}

	public HashSet<String> getTopKSet(){
		return candidates.get_topk();
	}

	public ArrayList<Double> getProximityVector(){
		ArrayList<Double> prunedList = new ArrayList<Double>();
		int index = 0;
		Iterator<Double> iter_prox = proximities.iterator();
		while(iter_prox.hasNext()){
			double val = iter_prox.next();
			if(index%250==0)
				prunedList.add(val);
			index++;
		}
		return prunedList;
	}

	public ArrayList<Integer> getVisited(){
		skr = new HashSet<Integer>();
		for(int i=0;i<Params.seeker.length;i++) skr.add(Params.seeker[i]);
		ArrayList<Integer> vst_u = new ArrayList<Integer>();
		for(int curr:vst){
			if(skr.contains(curr)) vst_u.add(curr);
		}
		this.visitedNodes=vst_u;
		return vst_u;

	}

	public String getNewResultsXML(boolean exact){
		String result="";
		result=String.format(Locale.US, "<ResultSet seeker=\"%s\" nbloops=\"%s\" isExact=\"%d\">", seeker, this.numloops, exact?1:0);

		result+=this.newXMLResults;
		if(!exact)
			result+=this.newBucketResults;
		result+=this.newXMLStats;
		result+="</ResultSet>\n";

		return result;
	}

	public String getResultsXML(){
		return this.newXMLResults;
	}

	public BasicSearchResult getResultsList(){
		return this.resultList;
	}

	public ArrayList<Integer> getViResult(){
		return this.visitedNodes;
	}

	public char[] getResultsForR() {
		char[] chaine=null;		
		//get results
		BasicSearchResult sr=new BasicSearchResult();
		sr.getResult();

		return chaine;
	}

}
