package org.dbweb.socialsearch.topktrust.datastructure.views;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;


public class UserView {
	
	private static String sqlGetScore = "select wscore, bscore from view_items where qid=? and item=?";
	
	private int qid;
	private HashSet<String> query;
	private double alpha;
	private ArrayList<ViewItem> viewItems = new ArrayList<ViewItem>();
	
	private ResultSet list;	
	private ResultSet bestlist;
	private Iterator<ViewItem> memlist;
	
	private double maxScore;
	private double minScore;
	private String headItem;
	
	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}
	
	public double getAlpha() {
		return alpha;
	}
	
	public void setQuery(HashSet<String> query) {
		this.query = query;
	}
	
	public HashSet<String> getQuery() {
		return query;
	}
	
	public void addViewItems(ViewItem item) {
		this.viewItems.add(item);
	}
	
	public void initListIterator(){
		memlist = viewItems.iterator();
	}
	
	public ViewItem getNextItem() throws SQLException {
//		ViewItem vitm = null;		
//		if(list.next()){
//			String itm = list.getString(1);
//			double ws = list.getDouble(2);
//			double bs = list.getDouble(3);
//			vitm = new ViewItem(itm, ws, ws, bs);
//		}
//		return vitm;
		ViewItem viewitm = null;
		if(memlist.hasNext()){
			viewitm = memlist.next();
		}
		return viewitm;
	}
	
	public ViewScore findItem(String id, Connection connection) throws SQLException{
//		ViewScore vsc = null;
//		PreparedStatement ps = connection.prepareStatement(sqlGetScore);
//		ps.setInt(1, qid);
//		ps.setString(2, id);
//		ResultSet result = ps.executeQuery();
//		if(result.next()){
//			double ws = result.getDouble(1);
//			double bs = result.getDouble(2);
//			vsc = new ViewScore(ws, bs);
//		}
		for(ViewItem vitm:viewItems)
			if(vitm.getId().equals(id))
				return new ViewScore(vitm.getWscore(),vitm.getBscore());
//		else
		return new ViewScore(0.0f, minScore); //maybe add the minimum value of this view?
//		result.close();
//		ps.close();
//		return vsc;
	}
	
	public void advanceBestList() throws SQLException{
		if(bestlist.next()){
			headItem = bestlist.getString(1);
			maxScore = bestlist.getDouble(3);
		}
		else{
			headItem = "";
			maxScore = minScore;
		}
	}

	public double getMaxScore() {
		return maxScore;
	}

	public String getHeadItem() {
		return headItem;
	}

	public void setQid(int qid) {
		this.qid = qid;
	}

	public int getQid() {
		return qid;
	}

	public void setMinScore(double minScore) {
		this.minScore = minScore;
	}

	public double getMinScore() {
		return minScore;
	}
	
	public String getQueryString(){
		String qry = "";
		int idx = 0;
		for(String tag:query){
			idx = idx + 1;
			qry = qry + tag + (idx<query.size()?" ":"");			
		}
		return qry;
	}

	public void setList(ResultSet list) {
		this.list = list;
	}
	
	public void setBestList(ResultSet list) {
		this.bestlist = list;
	}

}
