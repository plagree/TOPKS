/*
x * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbweb.socialsearch.topktrust.datastructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.TreeSet;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Math;

import org.dbweb.completion.trie.RadixTreeImpl;
import org.dbweb.socialsearch.shared.Methods;
import org.dbweb.socialsearch.topktrust.algorithm.score.Score;
import org.dbweb.socialsearch.topktrust.datastructure.comparators.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Silver
 */
public class ItemList implements Cloneable{

	private static Logger log = LoggerFactory.getLogger(ItemList.class);
	private HashMap<String,Item<String>> items; //this is a map (for fast access to item pointers)
	private HashSet<String> current_topk = new HashSet<String>();
	private DataDistribution d_distr;
	private DataHistogram d_hist;
	private Score score;

	private double min_from_topk;
	private double max_from_rest;
	public double getMax_from_rest() {
		return max_from_rest;
	}

	private int number_of_candidates;
	private int k1;
	private int num_users;
	private double error;
	private boolean views = false;

	private int k;

	private double bestScoreEstim = Double.POSITIVE_INFINITY;

	private HashMap<String,Double> normcontrib = new HashMap<String,Double>();
	private HashMap<String,Double> soccontrib = new HashMap<String,Double>();
	private double score_unseen;
	private String thritem;
	private PriorityQueue<Item<String>> sorted_items; 
	private boolean topk_changed = false;
	private double sumconf;
	private int ranking=0;

	private Comparator comparator;

	private FileWriter fil;

	public ItemList(){
		this.min_from_topk = 0;
		this.max_from_rest = 0;
		this.number_of_candidates = 0;
	}

	public ItemList(Comparator comparator, Score score, int num_users, int k, Item<String> virtualItem, DataDistribution d_distr, DataHistogram d_hist, double error){
		try {
			fil = new FileWriter("confidence.csv");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.min_from_topk = 0;
		this.max_from_rest = 0;
		this.number_of_candidates = 0;
		this.items = new HashMap<String,Item<String>>();
		this.sorted_items = new PriorityQueue<Item<String>>();
		this.comparator = comparator;
		this.k1 = k1;
		this.num_users = num_users;
		this.d_distr = d_distr;
		this.d_hist = d_hist;
		this.error = error;
		this.score = score;
		this.k = k;
	}
	

	public void removeItem(Item<String> item){
		this.items.remove(item.getItemId()+"#"+item.getCompletion());
		removeSortItem(this.sorted_items, item);
	}

	private static void removeSortItem(PriorityQueue<Item<String>> queue, Item<String> item) {
		final Iterator<Item<String>> it = queue.iterator();
		while(it.hasNext()) {
			final Item<String> current = it.next();
			if (current.getItemId().equals(item.getItemId()) 
					&& current.getCompletion().equals(item.getCompletion())) {
				it.remove();
				break;
			}
		}
	}
	
	public int getRankingItem(String item, int k) {
		this.removeDuplicates();
		ArrayList<Item<String>> sorted_av = new ArrayList<Item<String>>(sorted_items);
		int counter = 1;
		int res = 0;
		Collections.sort(sorted_av);
		for (Item<String> currItem: sorted_av) {
			//System.out.println("Data: "+currItem.getComputedScore()+" "+currItem.getCompletion()+" "+currItem.getItemId());
			if (item.equals(currItem.getItemId())) {
				System.out.println("Data: "+currItem.getComputedScore()+" "+currItem.getCompletion()+" "+currItem.getItemId());
				System.out.println("Counter: "+counter);
				res = counter;
				return res;
			}
			counter++;
		}
		return res;
	}
	
	private void removeDuplicates() {
		ArrayList<Item<String>> sorted_av = new ArrayList<Item<String>>(sorted_items);
		HashSet<String> uniqueItemIds = new HashSet<String>();
		Collections.sort(sorted_av);
		
		for (Item<String> item: sorted_av) {
			if (uniqueItemIds.contains(item.getItemId())) {
				this.removeItem(item);
			}
			else {
				uniqueItemIds.add(item.getItemId());
			}
		}
	}

	public ItemList(HashMap<String,Item<String>> itemList){
		this.min_from_topk = 0;
		this.max_from_rest = 0;
		this.number_of_candidates = 0;
		this.items = itemList;
	}

	public int getNumberOfSortedItems() {
		return this.sorted_items.size();
	}

	public void addItem(Item<String> item){
		this.items.put(item.getItemId()+"#"+item.getCompletion(), item);
		if(!item.isPruned()) this.sorted_items.add(item);
	}

	public Item<String> findItem(String itemId, String completion){
		if(items.containsKey(itemId+"#"+completion))
			return items.get(itemId+"#"+completion);
		else
			return null;    	
	}
	public HashMap<String,Item<String>> getItems(){
		return items;
	}

	public double getMaxContrib(String tag, double max){
		double contrib = max;
		for(Item<String> c_item:items.values()){
			double val = c_item.getContrib(tag);
			if(contrib<val) contrib=val;
		}
		return contrib;
	}

	public void setContribs(ArrayList<String> query, RadixTreeImpl trie){
		boolean exact = false;
		for(int i=0; i<query.size(); i++){
			exact = !((i+1)==query.size()); // exact if not the last word (not a prefix)
			this.normcontrib.put(query.get(i), (double)trie.searchPrefix(query.get(i), exact).getValue());
			this.soccontrib.put(query.get(i), (double)trie.searchPrefix(query.get(i), exact).getValue());
		}
	}

	public double getNormalContrib(String tag){
		if (normcontrib.containsKey(tag))
			return normcontrib.get(tag);
		return 0;
	}

	public double getSocialContrib(String tag){
		if (soccontrib.containsKey(tag))
			return soccontrib.get(tag);
		return 0;
	}

	public String getContribItem(){
		return thritem;
	}

	public void filterTopk(ArrayList<String> query) {
		PriorityQueue<Item<String>> filtered_items = new PriorityQueue<Item<String>>(sorted_items);
		String newPrefix = query.get(query.size()-1);
		String previousPrefix = newPrefix.substring(0, newPrefix.length()-1);
		// UPDATE OF KEYS ON PREVIOUS PREFIX
		if (this.soccontrib.containsKey(previousPrefix)) {
			if (this.soccontrib.get(previousPrefix) != null) {
				double value = this.soccontrib.get(previousPrefix);
				this.soccontrib.put(newPrefix, value);
			}
			else {
				this.soccontrib.put(newPrefix, null);
			}
			this.soccontrib.remove(previousPrefix);
		}
		if (this.normcontrib.containsKey(previousPrefix)) {
			if (this.normcontrib.get(previousPrefix) != null) {
				double value = this.normcontrib.get(previousPrefix);
				this.normcontrib.put(newPrefix, value);
			}
			else {
				this.normcontrib.put(newPrefix, null);
			}
			this.normcontrib.remove(previousPrefix);

		}
		// END OF UPDATE
		for (Item<String> item: filtered_items) {
			if (item.getCompletion().equals(""))
				continue;
			if (item.getCompletion().startsWith(newPrefix)) {
				item.updatePrefix(previousPrefix, newPrefix);
			}
			else {
				this.removeItem(item);
			}
		}
	}
	
	public void cleanForNewWord(ArrayList<String> query, RadixTreeImpl tag_idf, RadixTreeImpl trie, int approx) {
		String previousWord = query.get(query.size()-2);
		PriorityQueue<Item<String>> filtered_items = new PriorityQueue<Item<String>>(sorted_items);
		this.setContribs(query, trie);
		// END OF UPDATE
		for (Item<String> item: filtered_items) {
			if (item.getCompletion().equals(previousWord)) {
				//if (item.getItemId().equals("234841160923877376")) {
				//	System.out.println("before");
				//	item.debugging();
				//	item.computeWorstScore(1);
				//	System.out.println(item.getComputedScore());
				//}
				item.updateNewWord(query, tag_idf, approx);
				this.removeItem(item); // to change the keys in the HashMaps
				this.addItem(item);
				//if (item.getItemId().equals("234841160923877376")) {
				//	System.out.println("after");
				//	item.debugging();
				//	item.computeWorstScore(1);
				//	System.out.println(item.getComputedScore());
				//}
			}
			else {
				this.removeItem(item);
			}
		}
	}

	/**
	 * If time limit has been passed, we try to extract the best from what we found so far.
	 * @param k
	 * @param guaranteed
	 * @param possible
	 */
	public void extractProbableTopK(int k, HashSet<String> guaranteed, HashSet<String> possible) {
		int counter = 0;
		double wsc_t = 0;
		ArrayList<Item<String>> sorted_ws = new ArrayList<Item<String>>(sorted_items);
		Collections.sort(sorted_ws);
		ArrayList<Item<String>> sorted_bs = new ArrayList<Item<String>>(sorted_items);
		Collections.sort(sorted_bs, new ItemBestScoreComparator());
		ArrayList<Item<String>> possibleItems = new ArrayList<Item<String>>();

		if(sorted_ws.size() >= k)
			wsc_t = sorted_ws.get(k-1).getComputedScore();
		else {
			for (Item<String> item: sorted_ws)
				guaranteed.add(item.getItemId()+"#"+item.getCompletion());
			return;
		}
		for (Item<String> item: sorted_ws) {
			counter = 0;
			for (Item<String> item2: sorted_bs) {
				if (item.getComputedScore() < item2.getBestscore())
					counter += 1;
				else
					break;
			}
			if ((counter<=k) && (this.score_unseen<=item.getComputedScore()))
				guaranteed.add(item.getItemId()+"#"+item.getCompletion());
			else if(item.getBestscore() > wsc_t)
				possibleItems.add(item);
		}

		Collections.sort(possibleItems, new ItemAverageScoreComparator());
		int k_possible = k - guaranteed.size();
		Item<String> current_item;
		for (int i=0; i<k_possible; i++) {
			if (i >= possibleItems.size())
				break;
			current_item = possibleItems.get(i);
			possible.add(current_item.getItemId()+"#"+current_item.getCompletion());
		}
		return;
	}

	public double sample(int k, HashSet<String> possible, HashSet<String> newtopk, int samples, int position){
		//needs a MUCH better optimization
		double confidence = 1.0f;
		double val, val1;
		HashMap<String, Double> tk = new HashMap<String, Double>();
		ValueComparator tkvc =  new ValueComparator(tk);

		for(int round=0; round<samples; round++){
			HashMap<String, Double> normal = new HashMap<String, Double>();
			ValueComparator vc =  new ValueComparator(normal);
			TreeMap<String, Double> sorted = new TreeMap<String, Double>(vc);
			for(String itm:possible){
				double coeff_uni = Math.random();
				double coeff_exp = Math.log(1-Math.random())/(-1.0f);
				double ws = items.get(itm).getComputedScore();
				double bs = items.get(itm).getBestscore();
				double sc_uni = ws + (bs-ws)*coeff_uni;
				double sc_exp = ws + (bs-ws)*(1.0f-1.0f/(1.0f+coeff_exp));
				normal.put(itm, sc_uni);
			}		
			sorted.putAll(normal);
			int idx = 0;
			for(String itm:sorted.keySet()){
				val = normal.get(itm);
				if(!itm.equals("<unseen>")){
					if(idx<k){
						if(tk.containsKey(itm))
							tk.put(itm, tk.get(itm)+1.0f);
						else
							tk.put(itm, new Double(1.0f));
					}
					idx++;
				}
			}
		}

		TreeMap<String, Double> tksorted = new TreeMap<String,Double>(tkvc);
		tksorted.putAll(tk);
		int idx = 0;
		for(String itm:tksorted.keySet()){
			val = tk.get(itm);
			if(idx<k){
				confidence = confidence * (double)tk.get(itm)/(double)samples;
				sumconf += (double)tk.get(itm)/(double)samples;
				newtopk.add(itm);
			}
			idx++;
		}
		//    	try {
		//			fil.write(String.format("%d,%.5f,%d\n",position,confidence,possible.size()));
		//		} catch (IOException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}

		//    	log.info(String.format("maximal confidence %.5f - position %d",confidence,position));
		return confidence;
	}


	public boolean terminationCondition(ArrayList<String> query, float value, int k, int num_tags, float alpha, int num_users, RadixTreeImpl idf, HashMap<String,Integer> high, HashMap<String,Float> user_weights, HashMap<String,Integer> positions, int approx, boolean sortNeeded, boolean needUnseen, HashSet<String> guaranteed, HashSet<String> possible) throws IOException{
		//if(sortNeeded) Collections.sort(items,comparator);
		this.processBoundary(query, value,k, num_tags, alpha, num_users, idf, high, user_weights, positions, approx, sortNeeded, needUnseen, guaranteed, possible);
		if((this.max_from_rest<=this.min_from_topk)&&(number_of_candidates>=k)){
			if(fil!=null){
				try {
					fil.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return true;
		}
		else        	        
			return false;        
	}

	public void resetChange(){
		topk_changed = false;
	}

	public boolean topkChange(){
		return this.topk_changed;
	}

	private void processBoundary(ArrayList<String> query, float value, int k, int num_tags, float alpha, int num_users, RadixTreeImpl idf, HashMap<String,Integer> high, HashMap<String,Float> user_weights, HashMap<String, Integer> positions, int approx, boolean sortNeeded, boolean needUnseen, HashSet<String> guaranteed, HashSet<String> possible) throws IOException{
		HashMap<String, String> newtopk = new HashMap<String, String>();
		int number = 0;
		//int position = 0;
		double scoremin = 0.0f;
		double scoremax = 0.0f;
		thritem = null;
		topk_changed = false;
		if(needUnseen){ //Upper bound on unseen items
			for(String tag : query) {
				double contribution = 0;
				double uw = user_weights.get(tag);
				if((approx&Methods.MET_APPR_MVAR)==Methods.MET_APPR_MVAR){
					double euw = d_distr.getMean((String)tag)+Math.sqrt(d_distr.getVar((String)tag)/(float)(1.0f-Math.pow(1.0f-this.error,1.0f/(float)high.size())));
					uw = (uw>euw)?euw:uw;
				}
				else if((approx&Methods.MET_APPR_HIST)==Methods.MET_APPR_HIST){
					double euw = d_hist.getMaxEst((String)tag, (float)(1.0f-Math.pow(1.0f-this.error,1.0f/(float)(high.size()*high.get(tag)))));
					uw = (uw>euw)?euw:uw;
				}
				contribution = high.get(tag) * uw;
				double normalpart = alpha * high.get(tag);
				double socialpart = (1 - alpha) * contribution;
				double scorepart = normalpart + socialpart;
				scoremax += score.getScore(scorepart, idf.searchPrefix(tag, false).getValue());
				this.soccontrib.put(tag, (double)high.get(tag));
				this.normcontrib.put(tag, (double)high.get(tag));
			}
		}
		score_unseen = scoremax;

		scoremin = Double.POSITIVE_INFINITY;

		PriorityQueue<Item<String>> prioCopy = new PriorityQueue<Item<String>>(sorted_items);
		Item<String> curr_item=prioCopy.poll();
		int k1 = k - guaranteed.size(); //guaranteed.size()==0 if we do not use views
		sumconf = guaranteed.size();
		for(String itm:guaranteed) newtopk.put(itm, ""); // NOT WORKING
		while(curr_item!=null){       
			if(number<k1){
				if((needUnseen||possible.contains(curr_item.getItemId()))&&(!guaranteed.contains(curr_item.getItemId()))){
					if(!current_topk.contains(curr_item.getItemId())){
						topk_changed = true;
					}
					curr_item.computeBestScore(high, user_weights, positions, approx);
					String docId = curr_item.getItemId();
					if (!newtopk.containsKey(docId)) {
						newtopk.put(docId, curr_item.getCompletion()); // newtopk String to obj <docId, completion>
						scoremin = curr_item.getComputedScore();  
						number++;
					}
				}
			}
			else{
				if((needUnseen||possible.contains(curr_item.getItemId()))&&(!guaranteed.contains(curr_item.getItemId()))){
					curr_item.computeBestScore(high, user_weights, positions, approx);
					double curr_candidate = curr_item.getBestscore();
					//if(curr_candidate<=scoremin) curr_item.setPruned(true);
					if(scoremax<curr_candidate){
						scoremax = curr_candidate;
						this.normcontrib = curr_item.getNormalContrib();
						this.soccontrib = curr_item.getSocialContrib();
						this.thritem = curr_item.getItemId();
					}
				}
			}
			curr_item=prioCopy.poll();
		}

		if(views&&((approx&Methods.MET_ET)==Methods.MET_ET)){ //Using precomputed results -- calculating and estimation of the top-k
			newtopk = new HashMap<String, String>();
			//FileWriter filpos = new FileWriter(String.format("met%d_%s_%.2f_pos_%d.csv", approx, score.toString(), alpha, position));
			HashSet<String> possible_s = new HashSet<String>();
			if(possible.size()>0){
				for(String itm:possible){
					curr_item = items.get(itm);
					curr_item.computeBestScore(high, user_weights, positions, approx);
					if((curr_item.getBestscore()>scoremin)||(newtopk.containsKey(itm))){
						//filpos.write(String.format("%s\t%.5f\t%.5f\n",itm,curr_item.getComputedScore(),curr_item.getBestscore()));        			
						possible_s.add(itm);
					}
				}
			}
			else{
				for(String itm:items.keySet()){
					curr_item = items.get(itm);
					curr_item.computeBestScore(high, user_weights, positions, approx);        			
					if((curr_item.getBestscore()>scoremin)||(newtopk.containsKey(itm))){
						//filpos.write(String.format("%s\t%.5f\t%.5f\n",itm,curr_item.getComputedScore(),curr_item.getBestscore()));        			
						possible_s.add(itm);
					}
				}
			}
			//sample(k1,possible_s,newtopk,100,position); NOT WORKING WITH HASHMAP
			//for(String itm:guaranteed) newtopk.add(itm); NOT WORKING WITH HASHMAP
			number = k1;
			scoremin = Double.POSITIVE_INFINITY;
			scoremax = 0;
			log.info(String.format("expected precision %.5f", sumconf/(double)k));
		}
		this.number_of_candidates=number + guaranteed.size();
		this.min_from_topk=scoremin;
		this.max_from_rest=scoremax;
		this.current_topk = new HashSet<String>();
		for (String key: newtopk.keySet())
			this.current_topk.add(key+"#"+newtopk.get(key));
	}

	@Override
	public ItemList clone(){
		return new ItemList(this.items);
	}

	public HashSet<String> get_topk(){
		return this.current_topk;
		//    	HashSet<String> tk = new HashSet<String>();
		//    	for(Item<String> itm:topk) tk.add(itm.getItemId());
		//    	return tk;
	}

	public String toString(){
		String out = "";
		for(Item<String> item:items.values()){
			out = out + item.getItemId() + "\n";
		}
		return out;

	}

	public void setBestscoreestim(double score){
		bestScoreEstim = (bestScoreEstim>score)?score:bestScoreEstim;
	}

	public boolean isViews() {
		return views;
	}

	public void setViews(boolean views) {
		this.views = views;
	}
}
