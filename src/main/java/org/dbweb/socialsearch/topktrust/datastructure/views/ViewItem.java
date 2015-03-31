package org.dbweb.socialsearch.topktrust.datastructure.views;

import org.dbweb.socialsearch.topktrust.datastructure.Item;

public class ViewItem {
	private String id;
	private double sc;
	private double bs;
	private double ws;
	
	public ViewItem(String id, double sc, double ws, double bs){
		this.id = id;
		this.sc = sc;
		this.ws = ws;
		this.bs = bs;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setScore(double sc) {
		this.sc = sc;
	}

	public double getScore() {
		return sc;
	}
	
	public void setBscore(double bs) {
		this.bs = bs;
	}

	public double getBscore() {
		return bs;
	}

	public void setWscore(double ws) {
		this.ws = ws;
	}

	public double getWscore() {
		return ws;
	}

	@Override
    public boolean equals(Object o){
        if(o!=null)
            if(o instanceof ViewItem)
                if(((ViewItem) o).id == null ? this.id == null : ((ViewItem) o).id.equals(this.id))
                    return true;
        return false;
    }

	
	
}
