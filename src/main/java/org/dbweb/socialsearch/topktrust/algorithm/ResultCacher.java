package org.dbweb.socialsearch.topktrust.algorithm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.dbweb.socialsearch.general.connection.DBConnection;
import org.dbweb.socialsearch.shared.QueryEncoding;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathCompositionFunction;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathMinimum;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathMultiplication;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathPow;
import org.dbweb.socialsearch.topktrust.algorithm.score.BM25Score;
import org.dbweb.socialsearch.topktrust.algorithm.score.Score;
import org.dbweb.socialsearch.topktrust.algorithm.score.TfIdfScore;
import org.dbweb.socialsearch.topktrust.datastructure.Item;

public class ResultCacher {
	
	private static String sqlInsViews = "insert into view_queries(qid, seeker, alpha, func, scfunc, taggers, network,coeff,hidden) values(?,?,?,?,?,?,?,?,0)";
	private static String sqlInsViewQuery = "insert into view_keywords(qid,tag) values(?,?)";
	private static String sqlInsViewItems = "insert into view_items(qid,item, wscore, bscore) values(?,?,?,?)";
	private static String sqlGetMaxId = "select max(qid) from view_queries";
	
	private static Score[] score = {new TfIdfScore(), new BM25Score()};
	private static PathCompositionFunction[] func = {new PathMultiplication(), new PathMinimum(), new PathPow()};
	private static String[] network = {"soc_snet_tt","soc_snet_d","soc_snet_dt"};
	private double coeff = 2.0f;
	private static int[] seeker = {62280, 68930, 57754, 16340, 33658, 2933, 9151, 43132, 77541, 10662};
	
	public Connection connection;
	
	public ResultCacher(DBConnection con){
		connection = con.DBConnect();
	}
	
	public void cacheResult(QueryEncoding query, TreeSet<Item<String>> results) throws SQLException{
		int view_num = getViewNum(query);
		PreparedStatement ps;
		ps = connection.prepareStatement(sqlInsViews);
		ps.setInt(1,view_num);
		ps.setInt(2,seeker[query.getSeeker()]);
		ps.setDouble(3,query.getAlpha());
		func[query.getFunction()].setCoeff(query.getCoeff());
		ps.setString(4, func[query.getFunction()].toString());		
		ps.setString(5, score[query.getScore()].toString());
		ps.setString(6, "soc_tag_80");
		ps.setString(7, network[query.getNetwork()]);
		ps.setDouble(8, func[query.getFunction()].getCoeff());
		ps.executeUpdate();
		StringTokenizer tokenizer = new StringTokenizer(query.getQuery()," ");
		HashSet<String> qry = new HashSet<String>();
		while(tokenizer.hasMoreTokens()){
			qry.add(tokenizer.nextToken());
		}
		ps = connection.prepareStatement(sqlInsViewQuery);
		for(String tg:qry){
			ps.setInt(1, view_num);
			ps.setString(2, tg);
			ps.executeUpdate();
		}
		ps = connection.prepareStatement(sqlInsViewItems);
		for(Item it:results){
			ps.setInt(1, view_num);
			ps.setString(2, String.valueOf(it.getItemId()));
			ps.setDouble(3, it.getComputedScore());
			ps.setDouble(4, it.getBestscore());
			ps.executeUpdate();
		}
		connection.commit();
	}
	
	private int getViewNum(QueryEncoding query) throws SQLException{
		PreparedStatement ps = connection.prepareStatement(sqlGetMaxId);
		ResultSet rs = ps.executeQuery();
		int qid = 0;
		while(rs.next()){
			qid = rs.getInt(1);
		}
		return qid+1;
	}

}
