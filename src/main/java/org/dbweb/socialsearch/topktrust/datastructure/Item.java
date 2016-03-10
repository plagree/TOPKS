/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbweb.socialsearch.topktrust.datastructure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dbweb.completion.trie.RadixTreeImpl;
import org.dbweb.socialsearch.shared.Methods;
import org.dbweb.socialsearch.topktrust.algorithm.score.Score;
import org.dbweb.socialsearch.topktrust.datastructure.DataDistribution;
import org.dbweb.socialsearch.topktrust.datastructure.DataHistogram;

/**
 *
 * @author Silviu Maniu & Paul Lagrée
 */
public class Item<E> implements Comparable<Item<E>>{

	private long itemId = 0;
	private String completion;
	private Score score;

	private Map<E,Integer> tags = new HashMap<E,Integer>(); 		// tags of the query (useful?)
	private Map<E,Float> idf = new HashMap<E,Float>(); 				// idf for each term of the query
	private Map<E,Float> uf = new HashMap<E,Float>(); 				// number of users found for each term
	private Map<E,Integer> tdf = new HashMap<E,Integer>(); 			// number of users who tagged this item for each term (found in IL)
	private Map<E,Integer> nbUsersSeen = new HashMap<E,Integer>(); 	// number of users seen who tagged this item for each term

	private Map<E,Double> normcontrib = new HashMap<E,Double>();
	private Map<E,Double> soccontrib = new HashMap<E,Double>();

	private DataDistribution d_distr;
	private DataHistogram d_hist;

	private double error;
	private float alpha = 0;

	private double worstscore = 0.0f;
	private double bestscore = Double.POSITIVE_INFINITY;

	private boolean candidate = false;
	private boolean pruned = false;

	private double minscorefromviews = 0;
	private double maxscorefromviews = Double.POSITIVE_INFINITY;

	public Item(long itemId, float alpha, Score score, DataDistribution d_dist, DataHistogram d_hist, double error, String completion){
		this.itemId = itemId;
		this.alpha = alpha;
		this.d_distr = d_dist;
		this.d_hist = d_hist;
		this.error = error;
		this.score = score;
		this.completion = completion;
	}

	public Item(long itemId){
		this.itemId = itemId;
	}

	public String getCompletion() {
		return this.completion;
	}

	public Map<E,Float> getUf() {
		return this.uf;
	}

	public Map<E,Integer> getR() {
		return this.nbUsersSeen;
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
		this.nbUsersSeen.put(tag, 0);
	}

	public void updateIdf(E tag, float idf) {
		this.idf.put(tag, idf);
	}

	public void updateNewWord(List<E> query, RadixTreeImpl tag_idf, int approx) {
		for (int i=0; i<query.size()-1; i++) {
			this.idf.put(query.get(i), tag_idf.searchPrefix((String)query.get(i), true).getValue());
		}
		this.completion = "";
		this.computeWorstScore(approx);
	}

	public void copyValuesFirstWords(List<E> tagList, Item<E> itemToCopy) {
		for (int i = 0; i < tagList.size() - 1; i++) {
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
				this.nbUsersSeen.put(tag, itemToCopy.getR().get(tag));
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
		if (nbUsersSeen.containsKey(oldPrefix)) {
			nbUsersSeen.put(newPrefix, nbUsersSeen.get(oldPrefix));
			nbUsersSeen.remove(oldPrefix);
		}
	}

	public int updateScore(E tag, float value, int pos, int approx) {
		float prevUFVal = 0;    	
		if(this.uf.containsKey(tag))
			prevUFVal = uf.get(tag);
		uf.put(tag, prevUFVal + value);
		int prevR = 0;
		if(this.nbUsersSeen.containsKey(tag))
			prevR = nbUsersSeen.get(tag);
		nbUsersSeen.put(tag, prevR + 1);
		computeWorstScore(approx);
		return 0;
	}

	public int updateScoreDocs(E tag, int tdf, int approx) {
		if(!this.tdf.containsKey(tag))    		
			this.tdf.put(tag, tdf);
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

	public double computeBestScore(Map<String, ReadingHead> topReadingHead, Map<String, Float> user_weights, Map<String, Integer> positions, int approx){
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
			if (tdf.containsKey(tag)) { // value in IL
				bnormal=tdf.get(tag);
				stf = tdf.get(tag);
			}
			else if(topReadingHead.containsKey(tag) && (topReadingHead.get(tag) != null)){ // top value IL
				bnormal = topReadingHead.get(tag).getValue();
				stf = topReadingHead.get(tag).getValue();
				this.normcontrib.put(tag, bnormal);
			}
			if(uf.containsKey(tag)) bsocial=uf.get(tag); // score so far
			float stf_known = 0;
			if(nbUsersSeen.containsKey(tag)) stf_known = nbUsersSeen.get(tag); // users found who tagged so far
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

	public long getItemId(){
		return this.itemId;
	}

	public Map<E,Integer> returnTags(){
		return this.tags;
	}

	public void debugging() {
		System.out.println("Number of users seen: "+this.nbUsersSeen.toString());	  // number of users seen
		System.out.println("Tag in: "+this.tags.toString()); // 1 for tag in
		System.out.println("Sum of weights of users found: "+this.uf.toString());  // sum of weights of users found
		System.out.println("IDF of tag: "+this.idf.toString()); // IDF of tag
		System.out.println("Real TDF in IL: "+this.tdf.toString());  // real tdf in ILs
		System.out.println("alpha: "+this.alpha);
		System.out.println("score: "+this.score.toString());
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
			else if ((approx&Methods.MET_TOPKS) == Methods.MET_TOPKS) {
				if (nbUsersSeen.containsKey(tag))
					wnormal = nbUsersSeen.get(tag);
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
			if(nbUsersSeen.containsKey(tag))
				contrib -= nbUsersSeen.get(tag);
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
				uv = nbUsersSeen.get(tag);
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
				if(nbUsersSeen.containsKey(tag))
					wnormal = nbUsersSeen.get(tag);
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
		else{
			if(o.getBestscore()>this.getBestscore())
				return 1;
			else if(o.getBestscore()<this.getBestscore())
				return -1;
			else
				return Long.compare(o.getItemId(), itemId);
		}
	}

	@Override
	public boolean equals(Object o){
		if(!(o instanceof Item))
			return false;
		if (o == this)
			return true;
		@SuppressWarnings("unchecked")
		Item<E> rhs = (Item<E>)o;
		return new EqualsBuilder().append(this.itemId, rhs.itemId).isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder(17, 31).append(this.itemId).toHashCode();
	}

	@Override
	public String toString(){
		return String.valueOf(itemId);
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

	public float getSocialScore() {

		float socialScore = 0;

		for (E word: this.uf.keySet())
			socialScore += this.score.getScore( this.idf.get(word), this.uf.get(word) );
		return socialScore;
	}

	public float getTextualScore() {
		float textualScore = 0;
		for (E word : this.tags.keySet()) {
			if (this.tdf.containsKey(word)) { // the item has been seen in IL
				textualScore += this.score.getScore(this.idf.get(word), this.tdf.get(word));
			}
			else { // we haven't met this item in the IL of word yet
				textualScore += this.score.getScore(this.idf.get(word), this.nbUsersSeen.get(word));
			}
		}
		return textualScore;
	}

}
