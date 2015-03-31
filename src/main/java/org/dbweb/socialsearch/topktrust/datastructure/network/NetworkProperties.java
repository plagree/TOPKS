package org.dbweb.socialsearch.topktrust.datastructure.network;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

public class NetworkProperties {
	
	private Connection conn;
	private HashSet<String> query;
	private ArrayList<String> handles;
	private ArrayList<String> neigh;
	private ArrayList<Integer> types;
	private String network;
	
	public NetworkProperties(Connection con, HashSet<String> query, ArrayList<String> handles, ArrayList<Integer> types, ArrayList<String> neigh, String network){
		this.conn = con;
		this.handles = handles;
		this.query = query;
		this.types = types;
		this.network = network;
		this.neigh = neigh;
	}
	
	public String getJson() throws SQLException{
		int idx = 0;
		String output = "";
//		String output = "{";
		for(String node: neigh){
			output += "{";
			NodeProperties ndprop = new NodeProperties(conn, node, handles.get(idx), types.get(idx).intValue(), query, neigh, network);
			ndprop.getData();
			output += ndprop.getJson();
			output += "},";
			idx++;
		}
//		output += "}";
		return output;
	}
}
