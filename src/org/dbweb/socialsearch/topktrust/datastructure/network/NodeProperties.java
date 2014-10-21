package org.dbweb.socialsearch.topktrust.datastructure.network;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

import org.dbweb.socialsearch.topktrust.datastructure.UserLink;

public class NodeProperties {
	private static String sqlGetTaggedItems = "select count(*) from soc_tag_80 where \"user\"=? and tag=?";
	private static String sqlGetUserViews = "select qid, func, scfunc, alpha from view_queries where seeker=? and network=?";
	private static String sqlGetViewQuery = "select tag from view_keywords where qid=?";
	private static String sqlGetNeighbours = "select user2, weight from %s where user1=?";
	private static String sqlAllTaggedItems = "select item, tag from soc_tag_80 where \"user\"=? order by item asc, tag asc";
	private static String[] colors = {"#C74243", "#416D9C", "#70A35E"};
	
	private static String fmtItem = "link:\"%s\", tag:\"%s\"";
	private static String fmtView = "qid:\"%d\", query:\"%s\", func:\"%s\", score:\"%s\", alpha:\"%.2f\"";
	
	private Connection conn;
	private String node_id;
	private String handle;
	private HashSet<String> query;
	private String network;
	private ArrayList<String> neigh;
	private int type;
	
	private ArrayList<String> tags;
	private HashSet<String> viewTags;
	private ArrayList<UserLink<String,Double>> neighbours;
	private ArrayList<String> views;
	private ArrayList<String> items;
		
	public NodeProperties(Connection con, String node_id, String handle, int type, HashSet<String> query, ArrayList<String> neigh, String network){
		this.conn = con;
		this.node_id = node_id;
		this.handle = handle;
		this.query = query;
		this.type = type;
		this.network = network;
		this.neigh = neigh;
	}
	
	public void getData() throws SQLException{
		tags = new ArrayList<String>();
		viewTags = new HashSet<String>();
		neighbours = new ArrayList<UserLink<String,Double>>();
		items = new ArrayList<String>();
		views = new ArrayList<String>();
		
		PreparedStatement ps;
		ResultSet rs;
		
		//Getting neighbors
		ps = conn.prepareStatement(String.format(sqlGetNeighbours,network));
		ps.setInt(1, Integer.parseInt(node_id));
		rs = ps.executeQuery();
		while(rs.next()){
			String usr2 = rs.getString(1);
			double weight = rs.getDouble(2);
			if(neigh.contains(usr2))
				neighbours.add(new UserLink<String,Double>(node_id,usr2,weight));
		}
		rs.close();
		ps.close();
		
		//Getting items tagged with query tags
		for(String tag:query){
			ps = conn.prepareStatement(sqlGetTaggedItems);
			ps.setInt(1, Integer.parseInt(node_id));
			ps.setString(2, tag);
			rs = ps.executeQuery();
			int num = 0;
			if(rs.next()) num = rs.getInt(1);
			if(num>0) tags.add(tag);
			rs.close();
			ps.close();
		}
		
		//Getting all items
		ps = conn.prepareStatement(sqlAllTaggedItems);
		ps.setInt(1, Integer.parseInt(node_id));
		rs = ps.executeQuery();
		while(rs.next()){
			String itm = rs.getString(1);
			String tag = rs.getString(2);
			items.add(String.format(fmtItem,itm,tag));
		}
		rs.close();
		ps.close();
		
		
		//Getting views
		ps = conn.prepareStatement(sqlGetUserViews);
		ps.setInt(1, Integer.parseInt(node_id));
		ps.setString(2, network);
		rs = ps.executeQuery();
		while(rs.next()){
			int qid = rs.getInt(1);
			String func = rs.getString(2);
			String scfunc = rs.getString(3);
			double alpha = rs.getDouble(4);
			PreparedStatement ps1 = conn.prepareStatement(sqlGetViewQuery);
			ps1.setInt(1, qid);
			String qrystr = "";
			ResultSet rs1 = ps1.executeQuery();
			boolean found = false;
			while(rs1.next()){
				String tg = rs1.getString(1);
				if(query.contains(tg)) found=true;
				qrystr += tg + " ";
			}
			rs1.close();
			ps1.close();
			if(found){
				viewTags.add(qrystr);
				views.add(String.format(fmtView,qid,qrystr,func,scfunc,alpha));
			}
		}
		rs.close();
		ps.close();
				
	}
	
	public String getJson(){
		String neigh_format = "{ \"nodeTo\": \"%s\", \"nodeFrom\": \"%s\", \"data\": {\"weight\": %.5f}},";
		String output = "\"adjacencies\":[";
		for(UserLink<String,Double> lnk: neighbours){
			output += String.format(neigh_format, lnk.getRecipient(), lnk.getGenerator(), lnk.getWeight());
		}
		output += "],";
		output += "\"items\":[";
		for(String itm:items) output+="{"+itm+"},";
		output += "],";
		output += "\"views\":[";
		for(String vie:views) output+="{"+vie+"},";
		output += "],";
		output += String.format("\"data\":{ \"$color\": \"%s\", \"$type\": \"circle\",",colors[type]);
		output += "\"tags\": \"";
		for(String tag:tags) output+=tag+" ";
		output += "\",";
		output += "\"views\": \"";
		for(String qry: viewTags) output+="'"+qry+"'<br>";
		output += "\",";
		output += "},";
		output += String.format("\"id\": \"%s\", \"name\": \"%s\",", String.valueOf(node_id), handle);
		return output;
	}

}
