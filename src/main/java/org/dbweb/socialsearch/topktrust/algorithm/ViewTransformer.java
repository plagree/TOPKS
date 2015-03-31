package org.dbweb.socialsearch.topktrust.algorithm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.linear.LinearConstraint;
import org.apache.commons.math.optimization.linear.LinearObjectiveFunction;
import org.apache.commons.math.optimization.linear.Relationship;
import org.apache.commons.math.optimization.linear.SimplexSolver;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathCompositionFunction;
import org.dbweb.socialsearch.topktrust.algorithm.score.Score;
import org.dbweb.socialsearch.topktrust.datastructure.comparators.BScoreViewItemComparator;
import org.dbweb.socialsearch.topktrust.datastructure.comparators.WScoreViewItemComparator;
import org.dbweb.socialsearch.topktrust.datastructure.views.UserView;
import org.dbweb.socialsearch.topktrust.datastructure.views.ViewItem;
import org.dbweb.socialsearch.topktrust.datastructure.views.ViewScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.google.gwt.user.client.rpc.core.java.util.Collections;

public class ViewTransformer {
	
	public static Logger log = LoggerFactory.getLogger(ViewTransformer.class);
	
	private static String sqlGetViews = "select qid, alpha from view_queries where seeker=? and func=? and scfunc=? and hidden=0";
	private static String sqlGetViewQuery = "select tag from view_keywords where qid=?";
	private static String sqlGetViewItemsWS = "select item, wscore, bscore from view_items where qid=? order by wscore desc, bscore desc";
	private static String sqlGetViewItemsBS = "select item, wscore, bscore from view_items where qid=? order by bscore desc, wscore desc";
	private static String sqlGetItemScoreFromView = "select wscore, bscore from view_items where qid=? and item=?";
	private static String sqlGetMinItemFromView = "select min(wscore) from view_items where qid=?";
	
	private Connection connection;
	
	private int m;
	private PathCompositionFunction<Float> func;
	private String scfunc;
	private HashSet<String> query;
	private double alpha;
	private HashMap<String,Integer> tagFreqs;
	private HashMap<String,Float> idf;
	private Score score;
	
	private double maxScoreRest = Double.POSITIVE_INFINITY;
	
	private ArrayList<UserView> userviews;
	private ArrayList<String> tags;
	private HashMap<String, ViewScore> itemscores;
	private HashMap<String, ViewScore> maxscores;
	
	private PreparedStatement[] ps_ws;
	private PreparedStatement[] ps_bs;	
	private ResultSet[] head_ws;
	private ResultSet[] head_bs;
	
	private HashMap<String, ViewScore> candidates;
	private HashMap<String, ViewScore> guaranteed;
	private HashMap<String, ViewScore> possible;
	private boolean early;
	
	
	public ViewTransformer(int m, HashSet<String> query, double alpha, PathCompositionFunction func, String scfunc, String taggers, HashMap<String,Integer> tagFreqs, HashMap<String,Float> idf, String network, Score score,  Connection connection){
		this.connection = connection;
		this.m = m;
		this.func = func;
		this.scfunc = scfunc;
		this.query = query;
		this.alpha = alpha;
		this.tagFreqs = tagFreqs;
		this.score = score;
		this.idf = idf;
	}
	
	public boolean computeUsingViews(double proximity, ArrayList<UserView> uviews) throws SQLException{
		boolean exist = false;
		HashMap<Integer,ViewScore> scores = new HashMap<Integer,ViewScore>();
		HashMap<String,ViewScore> top = new HashMap<String,ViewScore>();
		guaranteed = new HashMap<String,ViewScore>();
		possible = new HashMap<String,ViewScore>();
		candidates = new HashMap<String,ViewScore>();		
		tags = new ArrayList<String>();
		getUserViews(uviews);
		double threshold = Double.POSITIVE_INFINITY;
		early = true;
		if(userviews.size()>0){
			exist = true;
			boolean finished = false;
			//Create array of tag indices
			HashSet<String> tag_univ = new HashSet<String>();
			tags = new ArrayList<String>();
			for(String tag:query)
				tag_univ.add(tag);
			for(UserView uview:userviews)
				for(String tag:uview.getQuery())
					tag_univ.add(tag);
			for(String tag:tag_univ)
				tags.add(tag);

			//TA adaptation
			boolean termination = false;
			double minScore = 0.0f;
			String minItem = "";
			do{
				finished = true;
				for(UserView uview:userviews){					
					ViewItem item = uview.getNextItem();
					if(item!=null){
						finished = false;
						if(!candidates.containsKey(item.getId())){
							itemscores = getScores(item.getId());
							double ws = coeff(proximity) * solveLP(true,false);						
							double bs = coeff(func.inverse((float)proximity)) * solveLP(false,false);
							if(ws>bs) bs=ws;
							double sc = ws;
							if((ws>=0)&&(bs!=Double.POSITIVE_INFINITY)){ //some items don't have a feasible solution
								if(top.size()>=m){
									if(sc>minScore){						
										top.remove(minItem);
										top.put(item.getId(), new ViewScore(ws,bs));
									}
								}
								else{
									top.put(item.getId(), new ViewScore(ws,bs));
								}
							}
							candidates.put(item.getId(), new ViewScore(ws,bs));
							minScore = Double.POSITIVE_INFINITY;
							for(String it:top.keySet()){
								ViewScore scv=top.get(it);							
								if(scv.getWscore()<minScore){
									minScore = scv.getWscore();
									minItem = it;
								}
							}							
						}
					}
				}
				setMaximalScores();
				threshold = coeff(func.inverse((float)proximity)) * solveLP(false,true);
				termination = (((top.size()>=m)&&(threshold<=minScore))||finished) ? true: false;
			}while(!termination);
			early = !finished;
			separe(minScore, threshold); // separing the candidate list into guaranteed and possible lists
		}
		closeAllLists();
		return exist;
	}
	

	
	private void getUserViews(ArrayList<UserView> uviews) throws SQLException{
		int column = 0;
		String qry = sqlGetViewItemsWS;
		PreparedStatement ps;
		ResultSet result;
		userviews = new ArrayList<UserView>();
//		ps_ws = new PreparedStatement[uviews.size()];
		ps_bs = new PreparedStatement[uviews.size()];
//		head_ws = new ResultSet[uviews.size()];
		head_bs = new ResultSet[uviews.size()];
		int idx = 0;
		for(UserView userview:uviews){			
			ps = connection.prepareStatement(sqlGetMinItemFromView);
			ps.setInt(1, userview.getQid());
			result = ps.executeQuery();
			if(result.next())
				userview.setMinScore(result.getDouble(1));			
//			ps_ws[idx] = connection.prepareStatement(sqlGetViewItemsWS);
//			ps_ws[idx].setInt(1, userview.getQid());
//			head_ws[idx] = ps_ws[idx].executeQuery();
//			userview.setList(head_ws[idx]);
			ps_bs[idx] = connection.prepareStatement(sqlGetViewItemsBS);
			ps_bs[idx].setInt(1, userview.getQid());
			head_bs[idx] = ps_bs[idx].executeQuery();
			userview.setBestList(head_bs[idx]);
			userview.advanceBestList();
			ps = connection.prepareStatement(qry);
			ps.setFetchSize(1000);
			ps.setInt(1, userview.getQid());
			result = ps.executeQuery();
			while(result.next()){				
				String itm = result.getString(1);
				double[] scores = new double[2];
				scores[0] = result.getDouble(2);
				scores[1] = result.getDouble(3);
				userview.addViewItems(new ViewItem(itm,scores[column],scores[0],scores[1]));
			}
			userview.initListIterator();
			userviews.add(userview);			
			idx = idx + 1;
			result.close();
			ps.close();
		}
	}
	
	
	
	private double solveLP(boolean minimize, boolean threshold){
		double solution = 0.0f;
		int numTags = tags.size();
		GoalType goal=GoalType.MINIMIZE;
		Relationship relation=Relationship.GEQ;
		if(!minimize){
			goal=GoalType.MAXIMIZE;
			relation=Relationship.LEQ;
			solution=Double.POSITIVE_INFINITY;
		}		
		
		//Objective function
		double params[] = new double[numTags];
		for(String tag:query)
			params[tags.indexOf(tag)] = 1.0f;
		LinearObjectiveFunction f = new LinearObjectiveFunction(params,0);
		
		//Constraints
		ArrayList<LinearConstraint> constraints = new ArrayList<LinearConstraint>();
		if(threshold){
			//Head of views constraints
			for(String qry:maxscores.keySet()){
				params = new double[numTags];
				for(String tag:qry.split(" "))
					params[tags.indexOf(tag)] = 1.0f;
				constraints.add(new LinearConstraint(params,relation,maxscores.get(qry).getBscore()));
			}
		}
		else{
			//UserView wscore constraints
			if(minimize){
				for(String qry:itemscores.keySet())
					if(itemscores.get(qry).getWscore()!=0){ //try to keep the constraints under control
						params = new double[numTags];					
						for(String tag:qry.split(" "))
							params[tags.indexOf(tag)] = 1.0f;
						constraints.add(new LinearConstraint(params,Relationship.GEQ,itemscores.get(qry).getWscore()));
					}
			}
			else{
				//UserView bscore constraints
				for(String qry:itemscores.keySet())
					if(itemscores.get(qry).getBscore()!=Double.POSITIVE_INFINITY){ //try to keep the constraints under control
						params = new double[numTags];
						for(String tag:qry.split(" "))
							params[tags.indexOf(tag)] = 1.0f;
						constraints.add(new LinearConstraint(params,Relationship.LEQ,itemscores.get(qry).getBscore()));
					}
			}
		}
		//maxtf constraints
		for(String tag:query){
			params = new double[numTags];
			params[tags.indexOf(tag)] = 1.0f;
			constraints.add(new LinearConstraint(params,Relationship.LEQ,score.getScore(tagFreqs.get(tag),idf.get(tag))));
		}					
		//>0 constraints
		for(String tag:tags){
			params = new double[numTags];
			params[tags.indexOf(tag)] = 1.0f;
			constraints.add(new LinearConstraint(params,Relationship.GEQ,0.0f));
		}
		
		//Solve
		try {
			org.apache.commons.math.optimization.RealPointValuePair sol = new SimplexSolver().optimize(f, constraints, goal, false);
			solution = sol.getValue();
		} catch (OptimizationException e) {
			log.info(String.format("issue in Simplex solver - %s",e.getMessage()));
		}
		
		return solution;
	}
	
	private HashMap<String,ViewScore> getScores(String item) throws SQLException{
		HashMap<String,ViewScore> scores = new HashMap<String,ViewScore>();
		//we use random access to get the scores in other views, if they exist
		for(UserView uview:userviews){
			double coeffMin = (alpha>=uview.getAlpha())?1.0f:alpha/uview.getAlpha();
			double coeffMax = (alpha>uview.getAlpha())?alpha/uview.getAlpha():1.0f;
			if(uview.getAlpha()==0.0f&&(alpha>uview.getAlpha())) coeffMax = Double.POSITIVE_INFINITY;
			ViewScore sc = uview.findItem(item, connection);
			String qstr = uview.getQueryString();
			double wsv = coeffMin * sc.getWscore();
			double bsv = coeffMax * sc.getBscore();
			//				double ws = 0.0f;
			//				double bs = Double.POSITIVE_INFINITY;
			//				PreparedStatement ps;
			//				ResultSet result;
			//				ps = connection.prepareStatement(sqlGetItemScoreFromView);
			//				ps.setInt(1, uview.getQid());
			//				ps.setString(2, item);
			//				result = ps.executeQuery();
			//				if(result.next()){
			//					ws = result.getDouble(1);
			//					bs = result.getDouble(2);
			//				}
			//				
			//				result.close();
			//				ps.close();
			if(scores.containsKey(qstr)){				
				ViewScore vsc = scores.get(qstr);
				double ws = wsv>vsc.getWscore()?wsv:vsc.getWscore();
				double bs = bsv<vsc.getBscore()?bsv:vsc.getBscore();
				if(ws>bs) bs=ws;
				scores.get(qstr).setWscore(ws);
				scores.get(qstr).setBscore(bs);
			}
			else{
				scores.put(qstr, new ViewScore(wsv,bsv));
			}
		}
		return scores;
	}
	
	private void setMaximalScores() throws SQLException{
		boolean stop = true;
		do{
			stop = true;
			for(UserView uview:userviews){
				if(candidates.containsKey(uview.getHeadItem())){
					uview.advanceBestList();
					stop = false;
				}	
			}
		}while(!stop);
		
		maxscores = new HashMap<String, ViewScore>();
		for(UserView uview:userviews){
			double coeffMax = (alpha>uview.getAlpha())?alpha/uview.getAlpha():1.0f;
			if(uview.getAlpha()==0.0f&&(alpha>uview.getAlpha())) coeffMax = Double.POSITIVE_INFINITY;
			String qstr = uview.getQueryString();
			double bsv = coeffMax * uview.getMaxScore();
			if(maxscores.containsKey(qstr)){				
				ViewScore vsc = maxscores.get(qstr);				
				double bs = bsv<vsc.getBscore()?bsv:vsc.getBscore();
				vsc.setBscore(bs);
			}
			else{
				maxscores.put(qstr, new ViewScore(0.0f,bsv));
			}
		}
	}
	
	private void separe(double minScore, double threshold){
		ArrayList<ViewItem> cand_bs = new ArrayList<ViewItem>();
		ArrayList<ViewItem> cand_ws = new ArrayList<ViewItem>();
		for(String itm:candidates.keySet()){
			double ws = candidates.get(itm).getWscore();
			double bs = candidates.get(itm).getBscore();
			cand_ws.add(new ViewItem(itm, ws, ws, bs));
			cand_bs.add(new ViewItem(itm, ws, ws, bs));			
		}
		java.util.Collections.sort(cand_ws, new WScoreViewItemComparator());
		java.util.Collections.sort(cand_bs, new BScoreViewItemComparator());
		for(ViewItem witm:cand_ws){
			int top = 0;
			for(ViewItem bitm:cand_bs)
				if((!witm.getId().equals(bitm.getId()))&&(witm.getWscore()<bitm.getBscore()))
					top++;
			if(witm.getWscore()<threshold) top++;
			top++;
			if(top<=m)
				guaranteed.put(witm.getId(), new ViewScore(witm.getWscore(),witm.getBscore()));			
			else if(witm.getBscore()>minScore)
				possible.put(witm.getId(), new ViewScore(witm.getWscore(),witm.getBscore()));
		}
		
	}
	
	private double coeff(double proximity){
		return (1.0f - proximity)*alpha + proximity;
	}

	public void setMaxScoreRest(double maxScoreRest) {
		this.maxScoreRest = maxScoreRest;
	}

	public double getMaxScoreRest() {
		return maxScoreRest;
	}

	public HashMap<String, ViewScore> getGuaranteed() {
		return guaranteed;
	}

	public HashMap<String, ViewScore> getPossible() {
		return possible;
	}

	public boolean isEarly() {
		return early;
	}
	
	private void closeAllLists() throws SQLException{
		for(int i=0;i<ps_bs.length;i++){
//			head_ws[i].close();
			head_bs[i].close();
//			ps_ws[i].close();
			ps_bs[i].close();			
		}
	}

}
