package org.dbweb.Arcomem.Integration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.dbweb.socialsearch.shared.Params;
import org.dbweb.socialsearch.topktrust.algorithm.paths.Network;

public class LoadIntoMemory {
	/**
	 *  Load data in main memory
	 *   - network
	 *   - user spaces (HashMap<user, HashMap< tag, HashSet<docs>>>)
	 *   - inverted lists
	 *  Quite long, that's why it's done only once
	 */
	
	public static ArrayList<String> dictionary;
	public static HashMap<String,Integer> high_docs;
	public static HashMap<String,Float> positions;
	public static HashMap<String,Float> userWeights;
	public static HashMap<String,Integer> tagFreqs;    
	public static HashMap<String,Float> tag_idf;
	public static HashMap<String, String> next_docs;
	public static HashMap<String, ResultSet> docs;
	
	/**
	 * All data are loaded in these structures
	 */
	public static HashMap<Integer,HashMap<String,HashSet<String>>> docs_users;
	
	private static String sqlGetDocsListByTag = "select item,num from docs where tag=? order by num desc";
	private static String sqlGetTagFrequency = "select num from tagfreq where tag=?";
	private static String sqlGetAllDocumentsTemplate = "select * from %s";
	private static String sqlGetDifferentTags = "SELECT distinct tag FROM %s";

	public static void loadData(Connection conn) throws SQLException {

		// Build Dictionary
		long beforeLoadDic = System.currentTimeMillis();
		buildDictionary(conn);
		long afterLoadDic = System.currentTimeMillis();
		System.out.println("Dictionary load in memory in "+(afterLoadDic-beforeLoadDic)+" ms...");
		
		// Network
		long beforeLoadNet = System.currentTimeMillis();
		Network net = Network.getInstance(conn);
		long afterLoadNet = System.currentTimeMillis();
		System.out.println("Network load in memory in "+(afterLoadNet-beforeLoadNet)+" ms...");

		// User spaces
		long beforeLoadUS = System.currentTimeMillis();
		loadUserSpaces(conn);
		long afterLoadUS = System.currentTimeMillis();
		System.out.println("USs load in memory in "+(afterLoadUS-beforeLoadUS)+" ms...");
		
		// Inverted lists
		long beforeLoadIL = System.currentTimeMillis();
		loadInvertedLists(conn);
		long afterLoadIL = System.currentTimeMillis();
		System.out.println("ILs load in memory in "+(afterLoadIL-beforeLoadIL)+" ms...");

		return;
	}
	
	public static void loadUserSpaces(Connection connection) throws SQLException {
		String sqlGetAllDocuments = String.format(sqlGetAllDocumentsTemplate, Params.taggers);
		docs_users = new HashMap<Integer,HashMap<String,HashSet<String>>>();
		connection.setAutoCommit(false);
		Statement stmt = connection.createStatement();
		stmt.setFetchSize(1000);
		ResultSet result = stmt.executeQuery(sqlGetAllDocuments); //IMPORTANT
		//long time0 = System.currentTimeMillis();
		while(result.next()){
			int d_usr = result.getInt(1);
			String d_itm = result.getString(2);
			String d_tag = result.getString(3);
			if(!docs_users.containsKey(d_usr)){
				docs_users.put(d_usr, new HashMap<String,HashSet<String>>());
			}
			if(!docs_users.get(d_usr).containsKey(d_tag)){
				docs_users.get(d_usr).put(d_tag, new HashSet<String>());
			}
			docs_users.get(d_usr).get(d_tag).add(d_itm);
		}
	}

	private static void loadInvertedLists(Connection connection) throws SQLException{
		//int N = 305361;
		
		PreparedStatement ps;
		ResultSet result;
		float userWeight = 1.0f;
		int number_documents = 1570866;
		
		high_docs = new HashMap<String,Integer>();
		positions = new HashMap<String,Float>();
		userWeights = new HashMap<String,Float>();
		//ArrayList<Double> proximities = new ArrayList<Double>();
		tagFreqs = new HashMap<String,Integer>();
		//HashMap<String,Integer> lastpos = new HashMap<String,Integer>();
		//HashMap<String,Float> lastval = new HashMap<String,Float>();    	    
		tag_idf = new HashMap<String,Float>();
		next_docs = new HashMap<String, String>();
		// USEFUL ??? int [] pos = new int[N];
		docs = new HashMap<String, ResultSet>();
		//int index = 0;
		for(String tag:dictionary){
			/*
			 * INVERTED LISTS ARE HERE
			 */
			ps = connection.prepareStatement(sqlGetDocsListByTag);
			ps.setString(1, tag);
			docs.put(tag, ps.executeQuery()); // INVERTED LIIIIIIIST 
			if(docs.get(tag).next()){
				int getInt2 = docs.get(tag).getInt(2);
				String getString1 = docs.get(tag).getString(1);
				high_docs.put(tag, getInt2);
				next_docs.put(tag, getString1);
			}
			else{
				high_docs.put(tag, 0);
				next_docs.put(tag, "");
			}
			positions.put(tag, 0f);
			userWeights.put(tag, userWeight);
			ps = connection.prepareStatement(sqlGetTagFrequency);
			ps.setString(1, tag);
			result = ps.executeQuery();
			int tagfreq = 0;
			if(result.next()) tagfreq = result.getInt(1);
			tagFreqs.put(tag, high_docs.get(tag));
			float tagidf = (float) Math.log(((float)number_documents - (float)tagfreq + 0.5)/((float)tagfreq+0.5));
			tag_idf.put(tag, new Float(tagidf));
		}
	}
	
	private static void buildDictionary(Connection connection) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        dictionary = new ArrayList<String>();
		
        try {
        	String sqlRequest = String.format(sqlGetDifferentTags, Params.taggers);
			ps = connection.prepareStatement(sqlRequest);
	        rs = ps.executeQuery();
	        while(rs.next()) {
	        	dictionary.add(rs.getString(1));
	        } 
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
