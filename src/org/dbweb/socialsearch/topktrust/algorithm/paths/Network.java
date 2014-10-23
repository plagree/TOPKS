package org.dbweb.socialsearch.topktrust.algorithm.paths;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import org.dbweb.socialsearch.shared.Params;
import org.dbweb.socialsearch.topktrust.datastructure.UserLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Network {
	private static Logger log=LoggerFactory.getLogger(Network.class);
	private static Network  _instance = null;
	private static String net_tab = null;
	private static HashMap<String,HashMap<Integer,ArrayList<UserLink<Integer,Float>>>> networks;
	protected static String sqlGetNetworkLinksTemplate = "select user1, user2, weight from %s order by user1 asc, weight desc";

	private Network(Connection connection) {
		Network.networks = new HashMap<String,HashMap<Integer,ArrayList<UserLink<Integer,Float>>>>();
		PreparedStatement ps;
		ResultSet result;
		try{
			for(int i=0;i<Params.network.length;i++){
				networks.put(Params.network[i], new HashMap<Integer,ArrayList<UserLink<Integer,Float>>>());
				String sqlQuery = String.format(sqlGetNetworkLinksTemplate, Params.network[i]);
				ps = connection.prepareStatement(sqlQuery);
				ps.setFetchSize(1000);
				result = ps.executeQuery();
				while(result.next()){
					int user1 = result.getInt(1);
					int user2 = result.getInt(2);
					float weight = result.getFloat(3);
					UserLink<Integer,Float> link = new UserLink<Integer,Float>(user1, user2, weight);
					ArrayList<UserLink<Integer,Float>> nlist;
					if(networks.get(Params.network[i]).containsKey(user1))
						nlist = networks.get(Params.network[i]).get(user1);
					else{
						nlist = new ArrayList<UserLink<Integer,Float>>();
						networks.get(Params.network[i]).put(user1, nlist);
					}
					nlist.add(link);
					link = new UserLink<Integer,Float>(user2, user1, weight);
					if(networks.get(Params.network[i]).containsKey(user2))
						nlist = networks.get(Params.network[i]).get(user2);
					else{
						nlist = new ArrayList<UserLink<Integer,Float>>();
						networks.get(Params.network[i]).put(user2, nlist);
					}
					nlist.add(link);
				}
				result.close();
				ps.close();
			}
		}
		catch(SQLException e){
			log.error(e.getMessage());
			System.exit(-1);
		}
	}

	public static synchronized Network getInstance(Connection connection) {
		if(null == _instance){
			_instance = new Network(connection);
		}
		return _instance;
	}

	public HashMap<Integer,ArrayList<UserLink<Integer,Float>>> getNetwork(String net){
		return networks.get(net);
	}

	public String getNetworkTable(){
		return net_tab;
	}
}
