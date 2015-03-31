package org.dbweb.socialsearch.topktrust.datastructure;

import java.util.ArrayList;
import java.util.HashMap;

public class DataDistribution {
	
	private double mean;
	private double variance;
	private double n_total;
	
	private HashMap<String,Double> pos;
	private HashMap<String,Double> tot_2;
	private HashMap<String,Double> tot;
	
	public DataDistribution(double mean, double variance, double n_total, ArrayList<String> query){
		this.mean = mean;
		this.variance = variance;
		this.n_total = n_total;
		this.pos = new HashMap<String,Double>();
		this.tot_2 = new HashMap<String,Double>();
		this.tot = new HashMap<String,Double>();
		for(String tag:query){
			pos.put(tag, 1.0);
			tot.put(tag, 0.0);
			tot_2.put(tag, 0.0);
		}
	}
	
	
	public void setPos(String tag, double value, double position){
		pos.put(tag, new Double(position));
		if(!tot.containsKey(tag)){
			tot.put(tag, 0.0);
			tot_2.put(tag, 0.0);
		}
		double p_tot_2 = tot_2.get(tag);
		double p_tot = tot.get(tag);
		p_tot_2+=value*value;
		p_tot+=value;
		tot.put(tag, p_tot);
		tot_2.put(tag, p_tot_2);
	}
	
	private double getKnownMean(String tag){
		double km = tot.get(tag)/pos.get(tag);
		return km>0?km:0;
	}
	
	private double getKnownVar(String tag){
		double kv = tot_2.get(tag)/pos.get(tag)-Math.pow(getKnownMean(tag), 2);
		return kv>0?kv:0;
	}
	
	public double getMean(String tag){		
		double m = (n_total * mean - tot.get(tag))/(n_total - pos.get(tag));
		return m>0?m:0;
	}
	
	public double getVar(String tag){
		double k_pos = pos.get(tag);
		double k_mean = getKnownMean(tag);
		double k_var = getKnownVar(tag);
		double v = (n_total*(mean*mean + variance)-
				k_pos*(Math.pow(k_mean,2)+k_var))/
				(n_total-k_pos)-Math.pow(this.getMean(tag),2);
		return v>0?v:0;
	}
	
}
