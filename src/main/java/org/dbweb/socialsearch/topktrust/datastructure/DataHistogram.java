package org.dbweb.socialsearch.topktrust.datastructure;

import java.util.ArrayList;
import java.util.HashMap;

public class DataHistogram {
	
	private int num_users;
	private int buckets;
	private ArrayList<Integer> histogram;
	
	private HashMap<String, Integer> pos;
	private HashMap<String, Float> val;
	
	public DataHistogram(int num_users, ArrayList<Integer> histogram)
	{
		this.num_users = num_users;
		this.histogram = histogram;
		this.buckets = histogram.size()-1;
		this.pos = new HashMap<String,Integer>();
		this.val = new HashMap<String,Float>();
	}
	
	public void setVals(String tag, int position, float value){
		this.pos.put(tag, position);
		this.val.put(tag, value);
	}
	
	public float getMaxEst(String tag, float delta)
	{
		return getEst(val.get(tag), pos.get(tag), val.get(tag), delta);
	}
	
	public float getMinEst(String tag, float delta)
	{
		float est=0.0f;
		//int val_bucket = buckets - (int)Math.floor(val.get(tag)*buckets);
		int val_bucket = buckets;
		float prob = 0.0f;
		int sum = 0;
		int rem_users = num_users - pos.get(tag);
		ArrayList<Integer> new_hist = getNewHist(pos.get(tag), val.get(tag));
		while((prob<(1-delta))&&val_bucket>=0){
			sum = new_hist.get(val_bucket);
			prob = 1-(float)sum/(float)rem_users;
			if(prob<=(1-delta)){
				est = 1.0f - (float)(val_bucket+1)/(float)(buckets);
				if(est<0) est = 0.0f;				
				val_bucket--;
			}
		}
		return est;
	}
	
	private float getEst(float est_init, int position, float value, float delta)
	{
		float est=est_init;
		int val_bucket = buckets - (int)Math.floor(value*buckets);
		float prob = 0.0f;
		int sum = 0;
		int rem_users = num_users - position;
		ArrayList<Integer> new_hist = getNewHist(position, value); //to be replaced by a conservative O(1) estimation
		
		while(prob<delta){
			sum = new_hist.get(val_bucket);
			prob = (float)sum/(float)rem_users;
			if(prob<delta){
				est = 1.0f - (float)(val_bucket+1)/(float)(buckets);
				if(est<0) est = 0.0f;				
				val_bucket++;
			}
		}
		return est;
	}
	
	private ArrayList<Integer> getNewHist(int position, float value){
		int val_bucket = buckets - (int)Math.floor(value*buckets);
		ArrayList<Integer> new_hist = (ArrayList<Integer>)histogram.clone();
		//bring the irrelevant items to the current bucket
		for(int i=0;i<val_bucket;i++){
			new_hist.set(i, 0);			
		}
		//removing the already found items
		int tot_to_remove = position;
		int cur_bucket = val_bucket;
		while(cur_bucket<=buckets){
			int num_cur = new_hist.get(cur_bucket);
			if(num_cur - tot_to_remove>=0)
				new_hist.set(cur_bucket,num_cur - tot_to_remove);						
			else
				new_hist.set(cur_bucket, 0);							
			cur_bucket++;
		}
		return new_hist;
	}
	
}
