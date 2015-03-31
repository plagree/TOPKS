package org.dbweb.socialsearch.topktrust.algorithm.paths;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.dbweb.socialsearch.general.connection.DBConnection;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathCompositionFunction;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathMultiplication;
import org.dbweb.socialsearch.topktrust.datastructure.UserEntry;
import org.dbweb.socialsearch.topktrust.datastructure.views.UserView;

public class LandmarkPathsComputing {

	private static String sqlGetLandmarks = "select * from landmarks_%s where func='path_mult' order by landmark asc, dist desc";
	
	private int lid = -1;
	private HashMap<Integer,Integer> depth;
	private ArrayList<Integer> landmarkIds;
	private HashMap<Integer,ArrayList<Integer>> landmarks;
	private HashMap<Integer,HashMap<Integer,Float>> paths;
	private HashSet<Integer> visited;
	
	private int seeker;
	private String network;
	private DBConnection dbConnection;
	
	private PathCompositionFunction pathComp = new PathMultiplication();
	
	float maxRest = 1.0f;
	
	public LandmarkPathsComputing(String network, DBConnection dbConnection){
		this.network = network;
		this.dbConnection = dbConnection;
		
		depth = new HashMap<Integer,Integer>();
		landmarks = new HashMap<Integer,ArrayList<Integer>>();
		paths = new HashMap<Integer,HashMap<Integer,Float>>();
		landmarkIds = new ArrayList<Integer>();
		visited = new HashSet<Integer>();
	}
	
	public void loadLandmarks() throws SQLException{
		Connection connection = dbConnection.DBConnect();		
		PreparedStatement ps = connection.prepareStatement(String.format(sqlGetLandmarks, network));
    	ResultSet result = ps.executeQuery();
    	while(result.next()){
    		int landmark = result.getInt(2);
    		int user = result.getInt(3);
    		float dist = result.getFloat(4);
//    		if(user==seeker) seekerValues.put(landmark, dist);
    		if(landmark!=user){
    			if(!landmarks.containsKey(landmark)){
    				depth.put(landmark, 0);
    				landmarkIds.add(landmark);
    				landmarks.put(landmark, new ArrayList<Integer>());
    				paths.put(landmark, new HashMap<Integer,Float>());
    			}
    			landmarks.get(landmark).add(user);
    			paths.get(landmark).put(user, dist);
    		}
    	}
    	result.close();
    	ps.close();
	}
	
	public UserEntry<Float> getNextUser(){
		UserEntry<Float> retUser = null;
		boolean found = false;
		boolean allnotfound = true;
		for(int landmark:landmarks.keySet()){
			int dep = depth.get(landmark);
			if(dep<landmarks.get(landmark).size()){
				allnotfound = false;
				break;
			}
		}
		if(!allnotfound){
			while(!found){
				lid = ((lid+1)%landmarks.size());
				int landmark = landmarkIds.get(lid);
				int depthCurrent = depth.get(landmark);
				if(depthCurrent<landmarks.get(landmark).size()){
					int user = landmarks.get(landmark).get(depthCurrent);
					if(!visited.contains(user)){
						found = true;
						visited.add(user);
						retUser = new UserEntry<Float>(user,computeUserPath(user));
					}
					depth.put(landmark,depthCurrent+1);
				}
			}
		}
		lookIntoLists();
		computeMaxRemaining();
		return retUser;
	}
	
	public Float getMaxRemaining(){
		return maxRest;
	}
	
	public void setPathFunction(PathCompositionFunction func){
		this.pathComp = func;
	}

	public void setSeeker(int seeker){
		this.seeker = seeker;
		visited = new HashSet<Integer>();
		visited.add(seeker);
		lid = -1;
		for(int landmark:depth.keySet()) depth.put(landmark, 0);
	}
	
	private float computeUserPath(int user){
		float bestPath = 0.0f;
		for(int landmark:landmarks.keySet()){
			HashMap<Integer,Float> path = paths.get(landmark);
			float distLandmark = 0.0f;
			if(path.containsKey(user)) distLandmark = path.get(user);
			float distToLandmark = 0.0f;
			if(path.containsKey(seeker)) distToLandmark = path.get(seeker);
			float pathValue = (Float) pathComp.compute(distToLandmark,distLandmark);
			bestPath = pathValue>bestPath?pathValue:bestPath;
		}
		return bestPath;
	}
	
	private void lookIntoLists(){
		for(int landmark:landmarks.keySet()){
			boolean found = true;
			while(found){
				int dep = depth.get(landmark);
				if(dep<landmarks.get(landmark).size()){
					int user = landmarks.get(landmark).get(dep);
					if(visited.contains(user)){
						depth.put(landmark,dep+1);
					}
					else{
						found = false;
					}
				}
				else{
					found = false;
				}
			}
		}
	}
	
	private void computeMaxRemaining(){
		maxRest = 0.0f;
		for(int landmark:landmarks.keySet()){
			int dep = depth.get(landmark);
			if(dep<landmarks.get(landmark).size()){
				HashMap<Integer,Float> path = paths.get(landmark);
				int user = landmarks.get(landmark).get(dep);
				float distLandmark = 0.0f;
				if(path.containsKey(user)) distLandmark = path.get(user);
				float distToLandmark = 0.0f;
				if(path.containsKey(seeker)) distToLandmark = path.get(seeker);
				float pathNew = (Float) pathComp.compute(distToLandmark,distLandmark);
				maxRest = pathNew>maxRest?pathNew:maxRest;
			}
		}
	}
}
