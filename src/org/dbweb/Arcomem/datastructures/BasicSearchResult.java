package org.dbweb.Arcomem.datastructures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import com.google.common.collect.Multiset.Entry;
import com.google.common.primitives.Doubles;

/**
 * 
 * @author aminebaazizi
 *
 *ArrayList instead of Hashset 'cause it reads from topksAlgo which
 *arbitrarily ranks items based on their upper bounds
 * TODO smth strange which needs to be fixed: 
 * Result an ordered list of items 
 * Score stores an interval for each item 
 */
public class BasicSearchResult {

	ArrayList<String> topkComponent,bucketComponent;
	HashMap<String, double[]> Score; 
//	HashSet<String> Universe;
	protected int nbLoops;
	int k=-1;
	int cardUniv; //total items --useful in case bucketComponent not required
	float alpha;
	String seeker;

	public String getTop(){
		return topkComponent.get(0);
	}
	
	public String putTop(String str){
		String old="";
		if(topkComponent.size()>0)
			old=topkComponent.set(0, str);

		return old;
	}
	
	public String removeTop(){
		String str=this.getTop();
		topkComponent.remove(0);	
		return str;
	}
	
	public String getFlop(){
		return topkComponent.get(topkComponent.size()-1);
	}
	
	public String putFlop(String str){
		String old="";
		if(topkComponent.size()>0)
			old=topkComponent.set(topkComponent.size()-1, str);
		return old;
	}
	
	public String removeFlop(){
		String str=this.getFlop();
		topkComponent.remove(topkComponent.size()-1);
		return str;
	}
	
	
	public void concat(ArrayList<String> tail)
	{
		this.topkComponent.addAll(tail);
	}
	
	public void push(String tail){
		this.topkComponent.add(0, tail);
	}
	
	public void concat(String tail)
	{
		this.topkComponent.add(tail);
	}
	
	/**
	 * 
	 * @param X
	 * @param Y
	 * @return
	 * 0 false
	 * 1 true
	 * 2 both in bucket and ties
	 * -1 err
	 */
	public int isAbove(String X, String Y){		
		int res = -1;
		
		if(topkComponent.contains(X))
			if(topkComponent.contains(Y))
				if(topkComponent.indexOf(X)<topkComponent.indexOf(Y))
					res=1;
				else res=0;
			else res=1;
		else if(topkComponent.contains(Y))
			 	res=0;
			else 
				res=2;
		
		return res;
	}
	
	public float getAlpha() {
		return alpha;
	}

	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}

	public int getCardUniv() {
		return cardUniv;
	}

	public void setCardUniv(int cardUniv) {
		this.cardUniv = cardUniv;
	}



	
	public boolean isEmpty() {
		return this.topkComponent.isEmpty();
	}
	
	public int getK() {
		return k;
	}

	public void setK(int k) {
		this.k = k;
	}

	/**
	 * Methods used for outputting aggregation results
	 * standard output (two flags set to false): <Res id=auto-increment><item></item></Res>, onlu topk
	 * @param bucketAsWell 
	 *  wrap bucket items inside <flopk> element
	 * @param completeMD
	 *  <item minScore= maxScore drawnScore= ></item>
	 * 					
	 * @return
	 */
	
	public String outputXML(boolean bucketAsWell,  boolean completeMD) {
		String XMLRes = "<AggTOPK>\n";
		String[] itemPatterns= {"<item>%s</item>\n","<item minScore=\"%3d\" maxScore=\"%3d\" drawnScore=\"%3d\">%s</item>\n"};
		
		for(String item:this.topkComponent){
			if(completeMD)
				XMLRes+=String.format(itemPatterns[1], Score.get(item)[0],Score.get(item)[1],drawScore(item,100),item);
			else
				XMLRes+=String.format(itemPatterns[0],item);	
		}
		
		//output bucket if needed
		if(bucketAsWell){
			XMLRes+="<flopK>";
			for(String item:this.bucketComponent)
				XMLRes+=String.format(itemPatterns[0],item);
			XMLRes+="</flopK>\n";

		}
			
				
		XMLRes+="</AggTOPK>\n";	
		return XMLRes;
	}
	
//	public HashSet<String> getUniverse() {
//		return Universe;
//	}
//	public void setUniverse(HashSet<String> universe) {
//		Universe = universe;
//	}


	/*
	 * clear
	 */

	
	/*
	 * sorts Result according to  drawn scores
	 */
	public void linearize(int nbDraws){
//		HashMap<String, Double> tmpRes=new HashMap<String, Double>();
		double[] estScore={0,0};
		for(String str:this.topkComponent){
			estScore[0]=drawScore(str, nbDraws);//implicit use of Scores
			this.Score.put(str, estScore.clone());
		}
		this.sortItems();
	}
	/*
	 * draw score within interval score for a given item 
	 */
	public double drawScore(String item, int nbDraws){
		HashMap<Double, Integer> STATS = new HashMap<Double, Integer>();
		double drawnsc = 0, lowBound=this.Score.get(item)[0], range=this.Score.get(item)[1]-lowBound;
		int count=0, maxVal=0;
		for(int i=0;i<nbDraws;i++){
			drawnsc=lowBound+(Math.random()*range);
			
			if(STATS.containsKey(drawnsc))
				count=STATS.get(drawnsc);
			STATS.put(drawnsc, count++);//increment in both case
			count=0;	
		}
		maxVal=Collections.max(STATS.values());
		for(java.util.Map.Entry<Double, Integer> entry:STATS.entrySet())
			if(entry.getValue()==maxVal)drawnsc=entry.getKey();
		return drawnsc;
		
	}
	
	/**
	 * return lower bound
	 * @param index
	 * @return
	 */
	public double getScoreOf(int index)
	{
		return Score.get(topkComponent.get(index))[0];//
	}
	
	/**
	 * 
	 * @param index
	 * @return
	 */
	public double getUpScore(int index)
	{
		return Score.get(topkComponent.get(index))[1];//
	}
	
	
	/*
	 * sort based on lower bound
	 */
	public void sortItems() {
		String tmp="";
		for (int i = 1; i < this.topkComponent.size(); i++)
		      for (int j = i; j > 0; j--)
		      
		    	  if (this.getScoreOf(j-1)<this.getScoreOf(j))
			          {
		       	    	tmp=topkComponent.get(j-1);
		       	    	topkComponent.set(j-1, topkComponent.get(j));
		       	    	topkComponent.set(j, tmp);	
			          }
			         else break;
		      
		       	 
		      		         
		}
	




	public int getNbLoops() {
		return nbLoops;
	}

	public void setNbLoops(int nbLoops) {
		this.nbLoops = nbLoops;
	}

	public void printDetails(){
//		System.out.println("NBloop="+this.nbLoops);
		for (String item :this.topkComponent)
			System.out.println(item+" Score Range=["+ Score.get(item)[0]+","+ Score.get(item)[1]+"]");	
	}
	
	
	
//	public BasicSearchResult(ArrayList<String> result,
//			HashMap<String, Interval> score) {
//		super();
//		Result = result;
//		Score = score;
//	}

	



	ArrayList<Integer> visited;
	private long globalDistance;
	
	ArrayList<String> lkResult;//store result of local kemenization
	
	
	public ArrayList<String> getLkResult() {
		return lkResult;
	}

	public void setLkResult(ArrayList<String> lkResult) {
		this.lkResult = lkResult;
	}

	char kind;//k for kendall, s for spearman
	
 	public long getDistance() {
		return globalDistance;
	}

	public void setDistance(long distance) {
		this.globalDistance = distance;
	}

	public String getItem(int idx){
 		return this.topkComponent.get(idx);
 	}
 	
 	public int getItemIndex(String item){
 		return this.topkComponent.indexOf(item);
 	}
	
	public void setVisited(ArrayList<Integer> visited) {
		this.visited = visited;
	}

	public ArrayList<Integer> getVisited() {
		return visited;
	}

	public ArrayList<String> getResult() {
		return this.topkComponent;
	}

	public void setResult(ArrayList<String> result) {
		this.topkComponent = result;
	}

	public BasicSearchResult(String elem){
		super();
		this.topkComponent = new ArrayList<String>(Arrays.asList(elem));
		this.bucketComponent= new ArrayList<String>(); 
		this.k=1;
	}
	
	/**
	 * bucket comp empty by default
	 * @param result
	 */
	public BasicSearchResult(ArrayList<String> result) {
		super();
		this.topkComponent = result;
		this.bucketComponent= new ArrayList<String>(); 
		this.k=result.size();
	}

	public BasicSearchResult() {
		super();
		this.topkComponent = new ArrayList<String>();
		this.bucketComponent = new ArrayList<String>();
		this.Score= new HashMap<String, double[]>();
		this.k=10;
		
	}

	public BasicSearchResult(int K) {
		super();
		this.topkComponent = new ArrayList<String>();
		this.bucketComponent = new ArrayList<String>();

		this.Score= new HashMap<String, double[]>();
		this.k=K;
		
	}
	
	public void addResult(String item){
		this.topkComponent.add(item);
	}
	
	public void addResult(String item, double[] interval){
		this.topkComponent.add(item);
		this.Score.put(item, interval);		
	}
	
	public void addResult(String item, double lower, double upper){
		double[] interval= {lower, upper};
		this.topkComponent.add(item);
		this.Score.put(item, interval);		
	}
	
	
	public void addBucket(String item) {
		this.bucketComponent.add(item);

	}
	
	public String getCommaSepItems() {
		String out="";
		int c=0;
		for (String item:this.topkComponent){
			if(c==0){
				out=item;
				c++;
			}
			out+=String.format(",%s",item);
		}
			return out;
	}
	
	
	
public void printListID() {
	for (String i :this.topkComponent)
			System.out.println(" "+i);		
	System.out.println("");

	}



public void printListID(boolean withBucket) {
	
	printListID();
	
	if(withBucket){
		System.out.println("==========BUCKET COMPONENT=========");
		for (String str :this.bucketComponent)
			System.out.println(" "+str);		
		System.out.println("");
	}
	

	}

public void printListID(String name) {
	System.out.println(name);
	System.out.println("_____");
	for (String i :this.topkComponent)
			System.out.println(" "+i);		
	System.out.println("_____");

	}
public  void printGlobalDist() {
	System.out.println("global"+this.kind+"distance"+this.globalDistance);
}

public int count() {
	return this.topkComponent.size();
}

//public Interval getItemScore(String item){
//	//locate item first
//	return this.Score.get(this.getItemIndex(item));
//}
//	public void setItemScore(String item, Interval score){
//		this.Score.put(item, score);
//	}
	
}

