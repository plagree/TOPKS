/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbweb.socialsearch.topktrust.datastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.dbweb.completion.trie.RadixTreeImpl;
import org.dbweb.socialsearch.shared.Methods;
import org.dbweb.socialsearch.topktrust.algorithm.score.Score;
import org.dbweb.socialsearch.topktrust.datastructure.DataDistribution;
import org.dbweb.socialsearch.topktrust.datastructure.DataHistogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Silviu Maniu
 */
public class Item<E> implements Comparable<Item<E>>{

	private String itemId = "";
	private String completion;
	
	private Score score;

	private Map<E,Integer> tags = new HashMap<E,Integer>();
	private Map<E,Float> idf = new HashMap<E,Float>();
	
	private Map<E,Float> uf = new HashMap<E,Float>();
	public Map<E,Integer> tdf = new HashMap<E,Integer>();

	private Map<E,Integer> r = new HashMap<E,Integer>();

	private Map<E,Float> firstval = new HashMap<E,Float>();

	private Map<E,Double> normcontrib = new HashMap<E,Double>();
	private Map<E,Double> soccontrib = new HashMap<E,Double>();

	private DataDistribution d_distr;
	private DataHistogram d_hist;

	private double error;

	private int totalTags = 0;  
	private float alpha = 0;

	private double worstscore = 0.0f;
	private double bestscore = Double.POSITIVE_INFINITY;

	private boolean candidate = false;
	private boolean pruned = false;

	private double minscorefromviews = 0;
	private double maxscorefromviews = Double.POSITIVE_INFINITY;

	public Item(String itemId, float alpha, Score score, DataDistribution d_dist, DataHistogram d_hist, double error, String completion){
		this.itemId = itemId;
		this.alpha = alpha;
		this.d_distr = d_dist;
		this.d_hist = d_hist;
		this.error = error;
		this.score = score;
		this.completion = completion;
	}

	public Item(String itemId){
		this.itemId = itemId;
	}

	public String getCompletion() {
		return this.completion;
	}
	
	public Map<E,Float> getUf() {
		return this.uf;
	}
	
	public Map<E,Integer> getR() {
		return this.r;
	}
	
	public Map<E,Integer> getTdf() {
		return this.tdf;
	}

	//Public functionality methods
	public boolean isCandidate(){
		return this.candidate;
	}

	public void enable(){
		this.candidate = true;
	}

	public void disable(){
		this.candidate = false;
	}

	public void addTag(E tag, float idf){
		this.tags.put(tag, 1);
		this.idf.put(tag, new Float(idf));
	}

	public void updateIdf(E tag, float idf) {
		this.idf.put(tag, idf);
	}
	
	public void updateNewWord(ArrayList<E> query, RadixTreeImpl tag_idf, int approx) {
		for (int i=0; i<query.size()-1; i++) {
			this.idf.put(query.get(i), tag_idf.searchPrefix((String)query.get(i), true).getValue());
		}
		this.completion = "";
		this.computeWorstScore(approx);
	}
	
	public void copyValuesFirstWords(ArrayList<E >tagList, Item<E> itemToCopy) {
		for (int i=0; i<tagList.size()-1;i++) {
			E tag = tagList.get(i);
			if (itemToCopy.getSocialContrib().containsKey(tag)) {
				this.soccontrib.put(tag, itemToCopy.getSocialContrib().get(tag));
			}
			if (itemToCopy.getNormalContrib().containsKey(tag)) {
				this.normcontrib.put(tag, itemToCopy.getNormalContrib().get(tag));
			}
			if (itemToCopy.getTdf().containsKey(tag)) {
				this.tdf.put(tag, itemToCopy.getTdf().get(tag));
			}
			if (itemToCopy.getUf().containsKey(tag)) {
				this.uf.put(tag, itemToCopy.getUf().get(tag));
			}
			if (itemToCopy.getR().containsKey(tag)) {
				r.put(tag, itemToCopy.getR().get(tag));
			}
		}
	}

	public void updatePrefix(E oldPrefix, E newPrefix) {
		if (soccontrib.containsKey(oldPrefix)) {
			this.soccontrib.put(newPrefix, this.soccontrib.get(oldPrefix));
			this.soccontrib.remove(oldPrefix);
		}
		if (normcontrib.containsKey(oldPrefix)) {
			this.normcontrib.put(newPrefix, this.normcontrib.get(oldPrefix));
			this.normcontrib.remove(oldPrefix);
		}
		if (tags.containsKey(oldPrefix)) {
			this.tags.put(newPrefix, this.tags.get(oldPrefix));
			this.tags.remove(oldPrefix);
		}
		if (idf.containsKey(oldPrefix)) {
			this.idf.put(newPrefix, this.idf.get(oldPrefix));
			this.idf.remove(oldPrefix);
		}
		if (this.tdf.containsKey(oldPrefix)) {
			this.tdf.put(newPrefix, this.tdf.get(oldPrefix));
			this.tdf.remove(oldPrefix);
		}
		if (uf.containsKey(oldPrefix)) {
			this.uf.put(newPrefix, this.uf.get(oldPrefix));
			this.uf.remove(oldPrefix);
		}
		if (r.containsKey(oldPrefix)) {
			r.put(newPrefix, r.get(oldPrefix));
			r.remove(oldPrefix);
		}
	}

	public int updateScore(E tag, float value, int pos, int approx){
		float prevUFVal = 0;    	
		if(this.uf.containsKey(tag))
			prevUFVal = uf.get(tag);
		uf.put(tag, prevUFVal + value);
		int prevR = 0;
		if(this.r.containsKey(tag))
			prevR = r.get(tag);
		r.put(tag, prevR + 1);
		//computedScoreUpdated = false;
		computeWorstScore(approx);
		return 0;
	}

	public int updateScoreDocs(E tag, int tdf, int approx){
		if(!this.tdf.containsKey(tag))    		
			this.tdf.put(tag, tdf);    	
		//computedScoreUpdated = false;
		computeWorstScore(approx);
		return 0;
	}

	//Getters
	public double getComputedScore(){
		return (this.worstscore>this.minscorefromviews)?this.worstscore:this.minscorefromviews;
	}

	public double getBestscore(){
		return (this.bestscore>this.maxscorefromviews)?this.maxscorefromviews:this.bestscore;
	}

	public Map<E,Double> getNormalContrib(){
		return this.normcontrib;
	}

	public Map<E,Double> getSocialContrib(){
		return this.soccontrib;
	}

	public double computeBestScore(Map<String, Integer> high, Map<String, Float> user_weights, Map<String, Integer> positions, int approx){
		bestscore = 0;
		for(E tag : this.tags.keySet()){
			double uw = 0;
			if (user_weights.containsKey(tag))
				uw = user_weights.get(tag);
			double bsocial = 0; // social part of score (1-alpha)
			double bnormal = 0; // normal part (alpha)
			double bpartial = 0;
			double stf = 0;
			this.normcontrib.put(tag, 0.0);
			this.soccontrib.put(tag, 0.0);
			if(tdf.containsKey(tag)){ // value in IL
				bnormal=tdf.get(tag);
				stf = tdf.get(tag);
			}
			else if(high.containsKey(tag)){ // top value IL
				bnormal=high.get(tag);
				stf = high.get(tag);
				this.normcontrib.put(tag, bnormal);
			}
			if(uf.containsKey(tag)) bsocial=uf.get(tag); // score so far
			float stf_known = 0;
			if(r.containsKey(tag)) stf_known = r.get(tag); // users found who tagged so far
			this.soccontrib.put(tag, stf - stf_known);
			if((approx&Methods.MET_APPR_MVAR)==Methods.MET_APPR_MVAR){
				if(tdf.containsKey(tag)){
					double euw;
					if(stf-stf_known>0)
						euw = d_distr.getMean((String)tag)+Math.sqrt(d_distr.getVar((String)tag)/((stf - stf_known)*(float)(1.0f-Math.pow(1.0f-this.error,(float)1/(float)idf.size()))));
					else
						euw = 0;
					uw = (uw>euw)?euw:uw;
				}
				else{
					double euw = d_distr.getMean((String)tag)+Math.sqrt(d_distr.getVar((String)tag)/(float)(1.0f-Math.pow(1.0f-this.error,(float)1/(float)idf.size())));
					uw = (uw>euw)?euw:uw;
				}
				bsocial += (double)(stf - stf_known) * uw;   
			}
			else if((approx&Methods.MET_APPR_HIST)==Methods.MET_APPR_HIST){
				double euw;
				if(stf-stf_known>0)
					euw = d_hist.getMaxEst((String)tag, (float)(1.0f-Math.pow(1.0f-this.error,(float)1/(float)(idf.size()*(stf - stf_known)))));
				else
					euw = 0;
				uw = (uw>euw)?euw:uw;
				bsocial += (stf - stf_known) * uw;
			}
			else{
				bsocial += (stf - stf_known) * uw;
			}
			bpartial = alpha*bnormal + (1-alpha)*bsocial;
			bestscore += score.getScore(bpartial, idf.get(tag));
		}    	
		return this.bestscore;
	}

	public String getItemId(){
		return this.itemId;
	}

	public Map<E,Integer> returnTags(){
		return this.tags;
	}

	public float getMaxVal(String tag){
		if(this.firstval.containsKey(tag))
			return this.firstval.get(tag);
		else
			return 0;
	}

	//Private helper functions
	private int updateUnseenTags(E tag){
		if(this.tags.containsKey(tag)){
			Integer value=(Integer)this.tags.get(tag);
			int newValue = value.intValue()-1;
			if(newValue<0)
				return -1;
			else{
				this.tags.remove(tag);
				this.tags.put(tag, new Integer(newValue));
				this.totalTags--;
			}                
		}
		return 0;
	}
	
	public void debugging() {
		System.out.println(this.r.toString());	  // number of users seen
		System.out.println(this.tags.toString()); // 1 for tag in
		System.out.println(this.uf.toString());  // sum of weights of users found
		System.out.println(this.idf.toString()); // IDF of tag
		System.out.println(this.tdf.toString());  // real tdf in ILs
		System.out.println(this.alpha);
		System.out.println(this.score.toString());
	}
	
	public void computeWorstScore(int approx){
		float wscore = 0;
		for(E tag : this.idf.keySet()){
			float wsocial = 0;
			float wnormal = 0;
			float wpartial = 0;
			if(tdf.containsKey(tag)){
				wnormal=tdf.get(tag);
			}
			else if((approx&Methods.MET_TOPKS)==Methods.MET_TOPKS){
				if(r.containsKey(tag))
					wnormal=r.get(tag);
			}
			if(uf.containsKey(tag)){
				wsocial=uf.get(tag);
			}
			wpartial = alpha*wnormal + (1-alpha)*wsocial;
			wscore+=score.getScore(wpartial, idf.get(tag));
		}
		this.worstscore = wscore;
	}

	public double getContrib(String tag){
		double contrib = 0;
		if(tdf.containsKey(tag)){
			contrib += tdf.get(tag);
			if(r.containsKey(tag))
				contrib -= r.get(tag);
		}
		return contrib;
	}

	public double computeWorstScoreEstimate(int approx){
		double wscore = 0;
		for(E tag : this.idf.keySet()){
			double wsocial = 0;
			double wnormal = 0;
			double wpartial = 0;
			double stdf = 0;
			double uv = 0;
			double wpart_est = 0;    		
			if(uf.containsKey(tag)){
				wsocial=uf.get(tag);
				uv = r.get(tag);
			}
			if(tdf.containsKey(tag)){
				wnormal=tdf.get(tag);
				stdf = tdf.get(tag);
				double uw=0;
				if(stdf-uv>0){
					if((approx&Methods.MET_APPR_MVAR)==Methods.MET_APPR_MVAR)
						uw = this.d_distr.getMean((String)tag) - Math.sqrt(this.d_distr.getVar((String)tag)/((stdf-uv)*this.error));
					else if ((approx&Methods.MET_APPR_HIST)==Methods.MET_APPR_HIST)
						uw = this.d_hist.getMinEst((String)tag,(float)(1-Math.pow(1-this.error,1/idf.size())));
				}
				else
					uw = 0;
				uw = uw>0?uw:0;
				wpart_est = alpha*wnormal + (1-alpha)*wsocial + (1-alpha)*uw*(stdf-uv);
			}
			else if((approx&Methods.MET_TOPKS)==Methods.MET_TOPKS){
				if(r.containsKey(tag))
					wnormal=r.get(tag);
			}    		
			wpartial = alpha*wnormal + (1-alpha)*wsocial;
			wpartial = (wpartial>wpart_est)?wpartial:wpart_est;
			wscore+=score.getScore(wpartial, idf.get(tag));
		}
		this.worstscore = wscore;
		return wscore;
	}

	public int compareTo(Item<E> o) {
		if(o.getComputedScore()>this.getComputedScore())
			return 1;
		else if(o.getComputedScore()<this.getComputedScore())
			return -1;
		else if(o.getComputedScore()==this.getComputedScore()){
			if(o.getBestscore()>this.getBestscore())
				return 1;
			else if(o.getBestscore()<this.getBestscore())
				return -1;
			else
				return o.getItemId().compareTo(itemId);
		}
		else
			return 1;
	}

	@Override
	public boolean equals(Object o){
		if(o!=null)
			if(o instanceof Item)
				if(((Item<E>) o).itemId == null ? this.itemId == null : ((Item<E>) o).itemId.equals(this.itemId))
					return true;
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 67 * hash + (this.itemId != null ? this.itemId.hashCode() : 0);
		return hash;
	}

	@Override
	public String toString(){
		return itemId;

	}

	public void setMinScorefromviews(double scorefromviews) {
		this.minscorefromviews = (minscorefromviews<scorefromviews)?scorefromviews:minscorefromviews;
	}

	public double getMinScorefromviews() {
		return minscorefromviews;
	}

	public void setMaxScorefromviews(double scorefromviews) {
		this.maxscorefromviews = (maxscorefromviews>scorefromviews)?scorefromviews:maxscorefromviews;
	}

	public double getMaxScorefromviews() {
		return maxscorefromviews;
	}

	public boolean isPruned() {
		return pruned;
	}

	public void setPruned(boolean pruned) {
		this.pruned = pruned;
	}

}
