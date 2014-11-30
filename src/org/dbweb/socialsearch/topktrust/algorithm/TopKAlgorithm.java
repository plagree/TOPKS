/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbweb.socialsearch.topktrust.algorithm;


import org.apache.commons.collections4.trie.PatriciaTrie;
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
import org.dbweb.socialsearch.topktrust.algorithm.paths.Network;
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
import org.dbweb.completion.trie.RadixTreeImpl;
import org.dbweb.completion.trie.RadixTreeNode;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.SortedMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.util.Calendar;

/**
 *
 * @author Silviu & Paul
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
	protected static String sqlGetDifferentTags = "SELECT distinct tag FROM %s";

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

	protected HashMap<Integer, PatriciaTrie<HashSet<String>>> docs_users;

	protected RadixTreeImpl tag_idf;
	protected HashMap<String, ListIterator<UserEntry<Float>>> friends_list;
	protected ArrayList<UserEntry<Float>> friends;
	protected ArrayList<Float> values;
	protected PriorityQueue<UserEntry<Float>> prioQueue;
	protected HashMap<String,Integer> high_docs_query;
	protected HashMap<String, Integer> positions;
	protected HashMap<String,Float> userWeights;
	protected ArrayList<Double> proximities;
	protected HashMap<String,Integer> tagFreqs;
	protected HashMap<String,Float> maxval;
	protected HashSet<String> taggers;
	protected HashMap<String,ArrayList<UserView>> userviews;
	protected HashMap<String,HashSet<String>> unknown_tf;
	protected ArrayList<Integer> vst;
	protected HashSet<Integer> skr;
	protected HashMap<String, String> next_docs;
	protected ArrayList<String> dictionary;
	protected PatriciaTrie<String> dictionaryTrie;
	protected RadixTreeImpl completion_trie; // Completion trie

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

	//amine
	protected String newXMLResults="", newBucketResults="", newXMLStats="";

	BasicSearchResult resultList=new BasicSearchResult();
	double [] scoreInt = new double[2];

	public BasicSearchResult getResultList() {
		resultList.sortItems();
		return resultList;
	}

	public void setAlpha(float alpha) {
		this.alpha = alpha;
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

	private boolean firstPossible = true;
	private boolean needUnseen = true;
	private HashSet<String> guaranteed;
	private HashSet<String> possible;

	// NEW
	private HashMap<String, ArrayList<DocumentNumTag>> docs2;
	private HashMap<String, String> next_docs2;
	private int nbNeighbour;
	private ArrayList<Integer> queryNbNeighbour;

	private int numloops=0; //amine
	private boolean premiere = true;

	public TopKAlgorithm(DBConnection dbConnection, String tagTable, String networkTable, int method, Score itemScore, float scoreAlpha, PathCompositionFunction distFunc, OptimalPaths optPathClass, double error) throws SQLException{
		this.distFunc = distFunc;
		this.dbConnection = dbConnection;
		this.networkTable = networkTable;
		this.tagTable = tagTable;
		this.alpha = scoreAlpha;
		this.approxMethod = method;
		this.optpath = optPathClass;
		this.error = error;
		this.score = itemScore;

		long time_before_loading = System.currentTimeMillis();
		if (dbConnection != null)
			this.dbLoadingInMemory();
		else {
			try {
				this.fileLoadingInMemory();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		long time_after_loading = System.currentTimeMillis();
		System.out.println("File loading in "+(time_after_loading-time_before_loading)+" sec...");
	}

	public TopKAlgorithm(DBConnection dbConnection, String tagTable, String networkTable, int method, Score itemScore, float scoreAlpha, PathCompositionFunction distFunc, OptimalPaths optPathClass, double error, int number_documents, int number_users){
		this.distFunc = distFunc;
		this.dbConnection = dbConnection;
		this.networkTable = networkTable;
		this.tagTable = tagTable;
		this.alpha = scoreAlpha;
		this.approxMethod = method;
		this.optpath = optPathClass;
		this.error = error;
		this.score = itemScore;
		this.number_documents = Params.number_documents;
		this.number_users = Params.number_users;
	}


	/**
	 * Main call from TopKAlgorithm class, call this after building a new object to run algorithm
	 * This query method must be called for the FIRST prefix, when iterating to the next letter, use
	 * the method executeQueryPlusLetter
	 * @param seeker
	 * @param query
	 * @param k
	 * @return
	 * @throws SQLException
	 */
	public int executeQuery(String seeker, ArrayList<String> query, int k, int t, boolean newQuery) throws SQLException{
		//if (query.size() != 1)
		//	System.out.println("Query: "+query.toString());
		this.premiere=true;
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

		values = new ArrayList<Float>();
		taggers = new HashSet<String>();
		unknown_tf = new HashMap<String,HashSet<String>>();
		for(String tag:query)
			unknown_tf.put(tag, new HashSet<String>());
		this.optpath.setValues(values);
		this.optpath.setDistFunc(distFunc);
		if((this.approxMethod&Methods.MET_APPR_LAND)==Methods.MET_APPR_LAND){
			landmark.setSeeker(this.seeker);
			landmark.setPathFunction(this.distFunc);
			currentUser = new UserEntry<Float>(this.seeker,1.0f);
		}
		else{
			currentUser = optpath.initiateHeapCalculation(this.seeker, query);
		}

		userWeight = 1.0f;
		terminationCondition = false;
		PreparedStatement ps;
		ResultSet result;
		if (newQuery) {
			high_docs_query = new HashMap<String, Integer>();
			next_docs = new HashMap<String, String>();
			userWeights = new HashMap<String, Float>();
		}
		pos = new int[query.size()];

		String tag = query.get(query.size()-1);
		int index = 0;
		boolean exact = false;
		pos[index]=0;
		next_docs.put(tag, next_docs2.get(completion_trie.searchPrefix(tag, exact).getBestDescendant().getWord()));
		high_docs_query.put(tag, (int)completion_trie.searchPrefix(tag, false).getValue());
		index++;
		userWeights.put(tag, userWeight);

		proximities = new ArrayList<Double>();
		proximities.add((double)userWeight);

		//getting the userviews
		//String sqlGetViews = sqlGetViewsTemplate;
		//int idx=0;
		/*for(String tag:query){
			if(idx<query.size()-1){
				sqlGetViews+=String.format("\'%s\',", tag);
			}
			else{
				sqlGetViews+=String.format("\'%s\')", tag);
			}
			idx++;
		}*/

		if((this.approxMethod&Methods.MET_APPR_MVAR)==Methods.MET_APPR_MVAR){
			String sqlGetDistribution = String.format(sqlGetDistributionTemplate, this.networkTable);
			ps = connection.prepareStatement(sqlGetDistribution);
			ps.setInt(1, Integer.parseInt(seeker));
			ps.setString(2, this.distFunc.toString());
			result = ps.executeQuery();
			if(result.next()){
				double mean = result.getDouble(1);
				double variance = result.getDouble(2);
				this.d_distr = new DataDistribution(mean, variance, Params.number_users, query);
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
			this.d_hist = new DataHistogram(Params.number_users, hist);
		}
		
		this.nbNeighbour = 0;
		if (newQuery) {
			this.queryNbNeighbour = new ArrayList<Integer>();
			Comparator comparator = new MinScoreItemComparator();   
			virtualItem = createNewCandidateItem("<rest_of_the_items>",query,virtualItem,"");
			candidates = new ItemList(comparator, this.score, Params.number_users, k, this.virtualItem, this.d_distr, this.d_hist, this.error);
			candidates.setContribs(query, completion_trie);
		}
		else {
			// clean results obtained so far
			candidates.cleanForNewWord(query, this.tag_idf, completion_trie, this.approxMethod);
		}
		
		this.queryNbNeighbour.add(0);

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

		//long time0 = System.currentTimeMillis();
		mainLoop(k, seeker, query, t); /* MAIN ALGORITHM */
		//long time1 = System.currentTimeMillis();
		//System.out.println("Only mainLoop : "+(time1-time0)/1000+"sec.");

		return 0;
	}

	/**
	 * After answering a query session (initial prefix and possible completions), we need to
	 * reinitialize the trie and go back to the initial position of the tries.
	 * @param prefix
	 */
	public void reinitialize(String[] query, int length) { // TO CHECK
		String prefix = "";
		for (String keyword: query) {
			prefix = keyword.substring(0, length);
			SortedMap<String, String> completions = this.dictionaryTrie.prefixMap(prefix);
			Iterator<Entry<String, String>> iterator = completions.entrySet().iterator();
			Entry<String, String> currentEntry = null;
			userWeight = 1;
			while(iterator.hasNext()){
				currentEntry = iterator.next();
				String completion = currentEntry.getKey();
				if (positions.get(completion) == 0) {
					continue;
				}
				positions.put(completion, 0); // high_docs.put(completion, ); UPDATE EVERYTHING HERE
				DocumentNumTag firstDoc = docs2.get(completion).get(0);
				//high_docs.put(completion, firstDoc.getNum());
				//next_docs2.put(completion, firstDoc.getDocId());
				RadixTreeNode current_best_leaf = completion_trie.searchPrefix(completion, false).getBestDescendant();
				current_best_leaf.updatePreviousBestValue(firstDoc.getNum());
			}
		}
	}

	/**
	 * When a query with prefix was already answered, this method use previous work and answer prefix+l
	 * @param seeker
	 * @param query
	 * @param k
	 * @return
	 * @throws SQLException
	 */
	public int executeQueryPlusLetter(String seeker, ArrayList<String> query, int k, int t) throws SQLException{
		//if (query.size() != 1)
		//	System.out.println("Query+l: "+query.toString());
		this.premiere=true;
		String newPrefix = query.get(query.size()-1);
		String previousPrefix = newPrefix.substring(0, newPrefix.length()-1);
		this.updateKeys(previousPrefix, newPrefix);
		RadixTreeNode radixTreeNode = completion_trie.searchPrefix(newPrefix, false);
		if (radixTreeNode == null)
			return 0;
		String bestCompletion = radixTreeNode.getBestDescendant().getWord();
		ArrayList<DocumentNumTag> arr = docs2.get(bestCompletion);
		if (positions.get(bestCompletion) < arr.size()) {
			high_docs_query.put(newPrefix, arr.get(positions.get(bestCompletion)).getNum());
			next_docs.put(newPrefix, arr.get(positions.get(bestCompletion)).getDocId());
		}
		else {
			high_docs_query.put(newPrefix, 0);
			next_docs.put(newPrefix, "");
		}
		high_docs_query.remove(previousPrefix);
		next_docs.remove(previousPrefix);
		userWeights.put(newPrefix, userWeights.get(previousPrefix));
		userWeights.remove(previousPrefix);

		candidates.filterTopk(query);
		mainLoop(k, seeker, query, t);
		return 0;
	}

	/**
	 * MAIN LOOP of the TOPKS algorithm
	 * @param k : number of answers in the top-k
	 * @param seeker : 
	 * @param query
	 * @param t
	 * @throws SQLException
	 */
	protected void mainLoop(int k, String seeker, ArrayList<String> query, int t) throws SQLException{
		int loops=0;
		int skipped_tests = 10000; // Number of loops before testing the exit condition
		int steps = 1;
		boolean underTimeLimit = true;
		firstPossible = true;
		needUnseen = true;
		guaranteed = new HashSet<String>();
		possible = new HashSet<String>();
		long before_main_loop = System.currentTimeMillis();
		finished = false;
		do{
			docs_inserted = false;
			boolean social = false;
			boolean socialBranch = chooseBranch(query);
			if(socialBranch){
				processSocial(query);
				if(((this.approxMethod&Methods.MET_VIEW)==Methods.MET_VIEW)&&userviews.containsKey(currentUser.getEntryId())){
					boolean exist = viewTransformer.computeUsingViews(userWeight, userviews.get(currentUser.getEntryId()));
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
			}
			if(social) this.total_lists_social++;

			steps = (steps+1)%skipped_tests;
			if((steps==0)||(!needUnseen&&((approxMethod&Methods.MET_ET)==Methods.MET_ET))){
				try {
					/*
					 * During the terminationCondition method, look up at top_items of different ILs, we add
					 * them if necessary to the top-k answer of the algorithm.
					 */
					terminationCondition = candidates.terminationCondition(query, userWeight, k, query.size(), alpha, Params.number_users, tag_idf, high_docs_query, userWeights, positions, approxMethod, docs_inserted, needUnseen, guaranteed, possible);
				} catch (IOException e) {
					e.printStackTrace();
				}
				//For statistics only
				if(candidates.topkChange()){
					this.total_topk_changes++;
				}
				candidates.resetChange();
				long time_1 = System.currentTimeMillis();
				if ((time_1-before_main_loop)>t) {
					this.candidates.extractProbableTopK(k, guaranteed, possible);
					underTimeLimit = false;
				}
			}
			else{
				terminationCondition=false;
			}
			long time_1 = System.currentTimeMillis();
			if ((time_1-before_main_loop)>Math.max(t+25, t)) {
				underTimeLimit = false;
			}
			if (userWeight==0)
				terminationCondition = true;
			loops++;
		}while(!terminationCondition&&!finished&&underTimeLimit);
		//this.numloops=loops;
		//System.out.println("There were "+loops+" loops ...");
	}

	/**
	 * When alpha > 0, we need to alternate between Social Branch and Textual Branch
	 * This method selects which branch will be chosen in the current loop
	 * @param query
	 * @return true (social) , false (textual)
	 */
	protected boolean chooseBranch(ArrayList<String> query){
		double upper_social_score;
		double upper_docs_score;
		boolean textual = false;
		for(String tag:query){
			if((approxMethod&Methods.MET_TOPKS)==Methods.MET_TOPKS) {
				upper_social_score = (1-alpha)*userWeights.get(tag)*candidates.getSocialContrib(tag);
			}
			else
				upper_social_score = (1-alpha)*userWeights.get(tag)*tagFreqs.get(tag);
			if((approxMethod&Methods.MET_TOPKS)==Methods.MET_TOPKS)
				upper_docs_score = alpha*candidates.getNormalContrib(tag);
			else
				upper_docs_score = alpha*high_docs_query.get(tag);
			if(!((upper_social_score==0)&&(upper_docs_score==0))) finished = false;
			if((upper_social_score!=0)||(upper_docs_score!=0)) textual = textual || (upper_social_score<=upper_docs_score);
		}
		return !textual;
	}


	/**
	 * Social process of the TOPKS algorithm
	 */
	protected void processSocial(ArrayList<String> query) throws SQLException{
		int currentUserId;
		int index = 0;
		if(currentUser!=null) vst.add(currentUser.getEntryId());
		String tag;
		int nbNeighbourTag = 0;
		// for all tags in the query Q, triples Tagged(u,i,t_j)
		for(int i=0; i<query.size(); i++) {
			tag = query.get(i);
			nbNeighbourTag = this.queryNbNeighbour.get(i);
			if (nbNeighbourTag > this.nbNeighbour) {
				continue; // We don't need to analyse this word because it was already done previously
			}
			this.queryNbNeighbour.set(i, this.nbNeighbour+1);
			if(currentUser!=null){
				boolean found_docs = false;

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
				boolean TESTING = false;
				if(this.docs_users.containsKey(currentUserId) && !(currentUserId==seeker)){
					SortedMap<String, HashSet<String>> completions = docs_users.get(currentUserId).prefixMap(tag);
					if (completions.size()>0) {
						Iterator<Entry<String, HashSet<String>>> iterator = completions.entrySet().iterator();

						while(iterator.hasNext()){
							Entry<String, HashSet<String>> currentEntry = iterator.next();
							String completion = currentEntry.getKey();
							for(String itemId: currentEntry.getValue()) {
								found_docs = true;
								Item<String> item = candidates.findItem(itemId, completion);
								if (item==null) {
									Item<String> item2 = candidates.findItem(itemId, "");
									
									if (item2!=null) {
										item = this.createCopyCandidateItem(item2, itemId, query, item, completion);
									}
									else
										item = this.createNewCandidateItem(itemId, query,item, completion);
								}
								else {
									candidates.removeItem(item);
								}
								float userW = 0;
								
								userW = userWeight;
								item.updateScore(tag, userW, pos[index], approxMethod);
								candidates.addItem(item);								
								docs_inserted = true;
								total_documents_social++;                            
							}
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
				//float prev_part_sum = pos[index];
				if((approxMethod&Methods.MET_APPR_MVAR)==Methods.MET_APPR_MVAR)
					d_distr.setPos(tag, userWeight, pos[index]+1);
				else if((approxMethod&Methods.MET_APPR_HIST)==Methods.MET_APPR_HIST)
					d_hist.setVals(tag, pos[index]+1, userWeight);
				userWeights.put(tag, userWeight);
			}
			index++;
		}/* END FOR ALL TAGS IN QUERY Q */
		this.nbNeighbour++;
		if((this.approxMethod&Methods.MET_APPR_LAND)==Methods.MET_APPR_LAND){
			currentUser = landmark.getNextUser();
		}
		else{
			long time_loading_before = System.currentTimeMillis();
			currentUser = optpath.advanceFriendsList(currentUser);
			long time_loading_after = System.currentTimeMillis();
			long tl = (time_loading_after-time_loading_before)/1000;
			if (tl>1)
				System.out.println("Loading in : "+tl);
		}
		if(currentUser!=null)
			userWeight = currentUser.getDist().floatValue();
		else
			userWeight = 0.0f;
		proximities.add((double)userWeight);
	}

	/**
	 * We advance on Inverted Lists here (method for social branch)
	 * Given the new discovered items in User Spaces, do top-items can be updated?
	 * @param query ArrayList<String>
	 */
	private void lookIntoList(ArrayList<String> query){
		int index=0;
		boolean found = true;
		while (found) {
			String completion;
			String autre = completion_trie.searchPrefix(query.get(query.size()-1), false).getBestDescendant().getWord();;
			for(index=0;index<query.size();index++) {
				found = false;
				if (index == (query.size()-1)) //  prefix
					completion = completion_trie.searchPrefix(query.get(index), false).getBestDescendant().getWord();
				else {
					completion = query.get(index);
				}
				if(unknown_tf.get(query.get(index)).contains(next_docs.get(query.get(index))+"#"+completion)){
					Item<String> item1 = candidates.findItem(next_docs.get(query.get(index)), autre);
					if (item1==null) {
						unknown_tf.get(query.get(index)).remove(next_docs.get(query.get(index))+"#"+completion); // DON4T UNDERSTAND
						continue;
					}
					candidates.removeItem(item1);
					item1.updateScoreDocs(query.get(index), high_docs_query.get(query.get(index)), approxMethod);
					unknown_tf.get(query.get(index)).remove(next_docs.get(query.get(index))+"#"+completion); 
					if (index == (query.size()-1)) //  prefix
						advanceTextualList(query.get(index),index,false);
					else {
						advanceTextualList(query.get(index),index,true);
					}

					candidates.addItem(item1);
					found = true;
				}
			}
		}
	}

	/**
	 * We chose the textual branch (alpha>0), we advance in lists
	 * @param query
	 * @throws SQLException
	 */
	protected void processTextual(ArrayList<String> query) throws SQLException{
		int index = 0;
		RadixTreeNode currNode = null;
		String currCompletion;
		for(String tag:query){
			if (this.queryNbNeighbour.get(index)>this.nbNeighbour) {
				index++;
				continue;
			}
			if(!next_docs.get(tag).equals("")){
				currNode = completion_trie.searchPrefix(tag, false);
				currCompletion = currNode.getBestDescendant().getWord();
				Item<String> item = candidates.findItem(next_docs.get(tag), currCompletion);
				if(item==null) {
					Item<String> item2 = candidates.findItem(next_docs.get(tag), "");
					if (item2!=null) {
						item = this.createCopyCandidateItem(item2, next_docs.get(tag), query, item, currCompletion);
					}
					else
						item = this.createNewCandidateItem(next_docs.get(tag), query,item, currCompletion);
				}
				else
					candidates.removeItem(item);
				item.updateScoreDocs(tag, high_docs_query.get(tag), approxMethod);
				if(unknown_tf.get(tag).contains(item.getItemId()+"#"+currCompletion)) unknown_tf.get(tag).remove(item.getItemId()+"#"+currCompletion);
				candidates.addItem(item);
				docs_inserted = true;
				if ((index+1)==query.size()) // prefix, we don't search for exact match
					advanceTextualList(tag,index,false);
				else {
					advanceTextualList(tag,index,true);
				}
			}
			index++;
		}
	}

	/**
	 * Process with views, not used in the current project
	 * @param query
	 * @throws SQLException
	 */
	protected void processView(ArrayList<String> query) throws SQLException{
		HashMap<String,ViewScore> guar = viewTransformer.getGuaranteed();
		HashMap<String,ViewScore> need = viewTransformer.getPossible();
		boolean early = viewTransformer.isEarly();
		needUnseen = needUnseen && !early;
		for(String itm:guar.keySet()){
			Item<String> itm_c = candidates.findItem(itm, "");
			if(itm_c==null){
				itm_c = createNewCandidateItem(itm, query, itm_c,"");
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
				Item<String> itm_c = candidates.findItem(itm, "");
				if(itm_c==null){
					itm_c = createNewCandidateItem(itm, query, itm_c,"");
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

	/**
	 * Method to advance in inverted lists (using the trie)
	 * Used both in Social and Textual branches
	 * @param tag
	 * @param index
	 */
	protected void advanceTextualList(String tag, int index, boolean exact){

		RadixTreeNode current_best_leaf = completion_trie.searchPrefix(tag, exact).getBestDescendant();
		String word = current_best_leaf.getWord();
		ArrayList<DocumentNumTag> invertedList = docs2.get(word);
		positions.put(word, positions.get(word)+1);
		int position = positions.get(word);

		if(position < invertedList.size()){
			total_documents_asocial++;
			high_docs_query.put(tag, invertedList.get(position).getNum());
			next_docs.put(tag, invertedList.get(position).getDocId());
			current_best_leaf.updatePreviousBestValue(invertedList.get(position).getNum());
		}
		else{
			high_docs_query.put(tag, 0);
			next_docs.put(tag, "");
		}
	}

	/**
	 * Not used in current version
	 * @param item
	 * @param query
	 * @param completion
	 * @throws SQLException
	 */
	protected void getAllItemScores(String item, ArrayList<String> query, String completion) throws SQLException{
		Item<String> itm = candidates.findItem(item, completion);
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

	/**
	 * Creates a new candidate in the discovered items of the query.
	 * @param itemId
	 * @param tagList (tags of query)
	 * @param item
	 * @param completion
	 * @return
	 * @throws SQLException
	 */
	protected Item<String> createNewCandidateItem(String itemId, ArrayList<String> tagList, Item<String> item, String completion) throws SQLException{
		item = new Item<String>(itemId, this.alpha, Params.number_users, this.score,  this.d_distr, this.d_hist, this.error, completion);        
		int sizeOfQuery = tagList.size();
		int index = 0;
		for(String tag:tagList){
			index++;
			if (index < sizeOfQuery) {
				item.addTag(tag, tag_idf.searchPrefix(tag, true).getValue());
			}
			else {
				item.addTag(tag, tag_idf.searchPrefix(completion, true).getValue());
			}
			unknown_tf.get(tag).add(itemId+"#"+completion);
		}
		return item;
	}
	
	/**
	 * Creates a new candidate copy of the given one, which will be used for new prefix
	 * @param itemId
	 * @param tagList
	 * @param item
	 * @param completion
	 * @return
	 * @throws SQLException
	 */
	protected Item<String> createCopyCandidateItem(Item<String> itemToCopy, String itemId, ArrayList<String> tagList, Item<String> copy, String completion) throws SQLException{
		copy = new Item<String>(itemId, this.alpha, Params.number_users, this.score,  this.d_distr, this.d_hist, this.error, completion);        
		int sizeOfQuery = tagList.size();
		int index = 0;
		for(String tag:tagList){
			index++;
			if (index < sizeOfQuery) {
				copy.addTag(tag, tag_idf.searchPrefix(tag, true).getValue());
			}
			else {
				copy.addTag(tag, tag_idf.searchPrefix(completion, true).getValue());
			}
			unknown_tf.get(tag).add(itemId+"#"+completion);
		}
		copy.copyValuesFirstWords(tagList, itemToCopy);
		return copy;
	}

	/**
	 * Not used in current version
	 * @return
	 */
	public String statistics(){
		String tkpos="";
		return String.format(Locale.US, ""+
				"<br><stat><b>Time</b>: main loop <b>%.3f</b> sec</stat><br><br>"+
				"<stat><b>%d</b> total <b>user lists</b>, last proximity <b>%.3f</b></stat><br><br>"+
				"<stat><b>%d top-k changes</b>, last at position <b>%s</b></stat><br><br>"+
				"<stat><b>%d</b> docs in <b>user lists</b>, <b>%d</b> in <b>inverted lists</b>, <b>%d</b> random</stat><br><br>", 
				(float)time_loop/(float)1000,
				total_lists_social, this.userWeight,
				total_topk_changes, tkpos,
				total_documents_social, total_documents_asocial, total_rnd);
	}

	/**
	 * Not used in current version (used when exiting results in XML format)
	 */
	protected void setQueryResultsArrayList(ArrayList<String> query, String seeker, int k, int method, float alpha){
		System.out.println(this.candidates.getNumberOfSortedItems());
		System.out.println("this.candidates.get_topk().size()="+this.candidates.get_topk().size());
		String str="";
		this.newXMLResults = "<TopkResults>\n";

		if ((guaranteed.size()+possible.size())==0) {
			for(String itid:this.candidates.get_topk()){
				String[] split = itid.split("#");
				Item<String> item = candidates.findItem(split[0], split[1]);
				str=protectSpecialCharacters(item.getItemId());
				this.resultList.addResult(str, item.getComputedScore(), item.getBestscore());
				resultList.setNbLoops(this.numloops); //amine populate resultList object

				this.newXMLResults+=String.format(Locale.US,"<result  minscore=\"%.5f\" maxscore=\"%.5f\">%s</result>",
						item.getComputedScore(), item.getBestscore(), protectSpecialCharacters(item.getItemId()+"#"+item.getCompletion()));

				this.newXMLResults+="\n";
			}
		}
		else { // WE STOPPED BEFORE THE END OF THE ALGORITHM
			for(String itid: guaranteed){
				String[] split = itid.split("#");
				Item<String> item = candidates.findItem(split[0], split[1]);

				str=protectSpecialCharacters(item.getItemId());
				this.resultList.addResult(str, item.getComputedScore(), item.getBestscore());
				resultList.setNbLoops(this.numloops); //amine populate resultList object

				this.newXMLResults+=String.format(Locale.US,"<result  minscore=\"%.5f\" maxscore=\"%.5f\">%s</result>",
						item.getComputedScore(), item.getBestscore(), protectSpecialCharacters(item.getItemId()+"#"+item.getCompletion()));

				this.newXMLResults+="\n";
			}
			for(String itid: possible){
				String[] split = itid.split("#");
				Item<String> item = candidates.findItem(split[0], split[1]);
				str=protectSpecialCharacters(item.getItemId());
				this.resultList.addResult(str, item.getComputedScore(), item.getBestscore());
				resultList.setNbLoops(this.numloops); //amine populate resultList object

				this.newXMLResults+=String.format(Locale.US,"<result  minscore=\"%.5f\" maxscore=\"%.5f\">%s</result>",
						item.getComputedScore(), item.getBestscore(), protectSpecialCharacters(item.getItemId()+"#"+item.getCompletion()));

				this.newXMLResults+="\n";
			}
		}
		this.newXMLResults+="</TopkResults>\n";
	}

	/**
	 * Gives the ranking of a given item in the ranked list of discovered items
	 * @param item
	 * @param k
	 * @return
	 */
	public int getRankingItem(String item, int k) {
		return this.candidates.getRankingItem(item, k);
	}

	/**
	 * Method to update unknown_tf HashMap from query to query+l
	 * @param previousPrefix
	 * @param newPrefix
	 */
	private void updateKeys(String previousPrefix, String newPrefix) {
		if (unknown_tf.containsKey(previousPrefix)) {
			HashSet<String> old_unknown_tf = unknown_tf.get(previousPrefix);
			HashSet<String> new_unknown_tf = new HashSet<String>();
			for (String unknownDoc: old_unknown_tf) {
				if (unknownDoc.startsWith(newPrefix))
					new_unknown_tf.add(unknownDoc);
			}
			unknown_tf.put(newPrefix, new_unknown_tf);
		}
	}

	public void setLandmarkPaths(LandmarkPathsComputing landmark){
		this.landmark = landmark;
	}

	/**
	 * Not used in current version
	 * @param originalUnprotectedString
	 * @return
	 */
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
			results.add(candidates.findItem(itid, ""));
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
		BasicSearchResult sr=new BasicSearchResult();
		sr.getResult();

		return chaine;
	}

	/**
	 * Method to load file with triples in memory
	 * @throws IOException
	 */
	private void fileLoadingInMemory() throws IOException {
		this.completion_trie = new RadixTreeImpl(); // 
		this.positions = new HashMap<String, Integer>(); // positions for a given keyword in the graph (useful for multiple words)
		this.userWeights = new HashMap<String,Float>(); //DONE
		this.tagFreqs = new HashMap<String,Integer>(); //DONE BUT NOT USED
		this.tag_idf = new RadixTreeImpl(); //DONE
		this.next_docs2 = new HashMap<String, String>(); //DONE
		this.docs2 = new HashMap<String, ArrayList<DocumentNumTag>>(); //DONE
		this.docs_users = new HashMap<Integer, PatriciaTrie<HashSet<String>>>();
		this.dictionaryTrie = new PatriciaTrie<String>(); // trie on the dictionary of words
		userWeight = 1.0f;

		BufferedReader br;
		String line;
		String[] data;
		System.out.println("Beginning of file loading...");

		// Tag Inverted lists processing
		br = new BufferedReader(new FileReader(Params.dir+Params.ILFile));
		ArrayList<DocumentNumTag> currIL;
		int counter = 0;
		while ((line = br.readLine()) != null) {
			data = line.split("\t");
			if (data.length < 2)
				continue;
			String tag = data[0];
			if (!docs2.containsKey(data[0]))
				docs2.put(tag, new ArrayList<DocumentNumTag>());
			currIL = docs2.get(data[0]);
			for (int i=1; i<data.length; i++) {
				String[] tuple = data[i].split(":");
				if (tuple.length != 2)
					continue;
				currIL.add(new DocumentNumTag(tuple[0], Integer.parseInt(tuple[1])));
			}
			Collections.sort(currIL, Collections.reverseOrder());
			DocumentNumTag firstDoc = currIL.get(0);
			next_docs2.put(tag, firstDoc.getDocId());
			completion_trie.insert(tag, firstDoc.getNum());
			positions.put(tag, 0);
			userWeights.put(tag, userWeight); // ??
			tagFreqs.put(tag, firstDoc.getNum());
			counter++;
			if ((counter%50000)==0)
				System.out.println("\t"+counter+" tag ILs loaded");
		}
		br.close();

		System.out.println("Inverted List file loaded...");

		// Triples processing
		int userId;
		String itemId;
		String tag;
		br = new BufferedReader(new FileReader(Params.dir+Params.triplesFiles));
		counter = 0;
		System.out.println("Loading of triples");
		while ((line = br.readLine()) != null) {
			data = line.split("\t");

			if (data.length != 3)
				continue;
			userId = Integer.parseInt(data[0]);
			itemId = data[1];
			tag = data[2];
			if (!dictionaryTrie.containsKey(tag))
				dictionaryTrie.put(tag, "");
			if(!this.docs_users.containsKey(userId)){
				this.docs_users.put(userId, new PatriciaTrie<HashSet<String>>());
			}
			if(!this.docs_users.get(userId).containsKey(tag))
				this.docs_users.get(userId).put(tag, new HashSet<String>());
			this.docs_users.get(userId).get(tag).add(itemId);
			counter++;
			if ((counter%1000000)==0)
				System.out.println("\t"+counter+" triples loaded");
		}
		br.close();
		Params.number_users = this.docs_users.size();

		// Tag Freq processing
		br = new BufferedReader(new FileReader(Params.dir+Params.tagFreqFile));
		int tagfreq;
		while ((line = br.readLine()) != null) {
			data = line.split("\t");
			if (data.length != 2)
				continue;
			tag = data[0];
			tagfreq = Integer.parseInt(data[1]);
			float tagidf = (float) Math.log(((float)Params.number_documents - (float)tagfreq + 0.5)/((float)tagfreq+0.5));
			tag_idf.insert(tag, tagidf);
		}
		br.close();
	}

	/**
	 * Method to load triples from a database to memory
	 * @throws SQLException
	 */
	private void dbLoadingInMemory() throws SQLException{
		this.connection = dbConnection.DBConnect();
		PreparedStatement ps;
		ResultSet rs = null;
		this.dictionaryTrie = new PatriciaTrie<String>();

		// DICTIONARY
		dictionary = new ArrayList<String>();
		try {
			String sqlRequest = String.format(sqlGetDifferentTags, Params.taggers);
			ps = connection.prepareStatement(sqlRequest);
			rs = ps.executeQuery();
			while(rs.next()) {
				dictionary.add(rs.getString(1));
				dictionaryTrie.put(rs.getString(1), "");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Dictionary loaded, "+dictionary.size()+"tags...");

		// INVERTED LISTS
		ResultSet result;
		userWeight = 1.0f;
		completion_trie = new RadixTreeImpl();
		positions = new HashMap<String, Integer>();
		userWeights = new HashMap<String,Float>();
		tagFreqs = new HashMap<String,Integer>();
		int tagFreqDoc = 0;
		tag_idf = new RadixTreeImpl();
		next_docs2 = new HashMap<String, String>();
		HashMap<String, ResultSet> docs3 = new HashMap<String, ResultSet>();
		String[] dictionary2 = { // DEBUG PURPOSE
				"Obama", //twitter dump
				"Cancer",
				"Syria",
				"SOUGOFOLLOW",
				"Apple",
				"NoMatter",
				"SOUGOF",
				"SOUGOFOL",
				"TFBj",
				"TFBJ", 
				"TFBUSA",
				"TFB",
				"TFB_VIP",
				"TFBPH",
				"TFB_TeamFollow",
				"TFBINA", 
				"TFBFI", 
				"TFBjp", 
				"TFBJp", 
				"TFBJP",
				"openingact",
				"openingceremony"
		};
		for(String tag:dictionary2){
			/*
			 * INVERTED LISTS ARE HERE
			 */
			ps = this.connection.prepareStatement(sqlGetDocsListByTag);
			ps.setString(1, tag);
			docs3.put(tag, ps.executeQuery()); // INVERTED LIST
			if(docs3.get(tag).next()){
				int getInt2 = docs3.get(tag).getInt(2);
				String getString1 = docs3.get(tag).getString(1);
				tagFreqDoc = getInt2;
				next_docs2.put(tag, getString1);
				completion_trie.insert(tag, getInt2);
			}
			else{
				next_docs2.put(tag, "");
			}
			positions.put(tag, 0);
			userWeights.put(tag, userWeight);
			ps = connection.prepareStatement(sqlGetTagFrequency);
			ps.setString(1, tag);
			result = ps.executeQuery();
			int tagfreq = 0;
			if(result.next()) tagfreq = result.getInt(1);
			tagFreqs.put(tag, tagFreqDoc);
			float tagidf = (float) Math.log(((float)Params.number_documents - (float)tagfreq + 0.5)/((float)tagfreq+0.5));
			tag_idf.insert(tag, tagidf);

		}
		System.out.println("Inverted Lists loaded...");

		// USER SPACES
		sqlGetAllDocuments = String.format(sqlGetAllDocumentsTemplate, this.tagTable);
		int idx=0;

		for(String tag:dictionary2) {
			if(idx<dictionary2.length-1){
				sqlGetAllDocuments+=String.format("\'%s\',", tag);
			}
			else{
				sqlGetAllDocuments+=String.format("\'%s\')", tag);
			}
			idx++;
		}

		this.docs_users = new HashMap<Integer, PatriciaTrie<HashSet<String>>>();
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		stmt.setFetchSize(1000);
		result = stmt.executeQuery(sqlGetAllDocuments); //DEBUG PURPOSE, SMALL DATA SET
		while(result.next()){
			int d_usr = result.getInt(1);
			String d_itm = result.getString(2);
			String d_tag = result.getString(3);
			if(!this.docs_users.containsKey(d_usr)){
				this.docs_users.put(d_usr, new PatriciaTrie<HashSet<String>>());
			}
			this.docs_users.get(d_usr).put(d_tag, new HashSet<String>());
			this.docs_users.get(d_usr).get(d_tag).add(d_itm);
		}
		System.out.println("Users spaces loaded");
	}

}
