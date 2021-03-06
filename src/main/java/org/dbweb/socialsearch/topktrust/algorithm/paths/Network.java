package org.dbweb.socialsearch.topktrust.algorithm.paths;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dbweb.socialsearch.shared.Params;
import org.dbweb.socialsearch.topktrust.datastructure.UserLink;

public class Network {
    private static Network  _instance = null;
    private static String net_tab = null;
    private static Map<String,Map<Integer,List<UserLink<Integer,Float>>>> networks;

    private Network() throws NumberFormatException, IOException {
        Network.networks = new HashMap<String,Map<Integer,List<UserLink<Integer,Float>>>>();
        // Network processing
        String line;
        String[] data;
        int user1, user2;
        float weight;
        Params.numberOfNeighbours = new HashMap<String, Integer>();
        for(int i = 0; i < Params.network.length; i++){
            networks.put(Params.network[i], new HashMap<Integer,List<UserLink<Integer,Float>>>());
            BufferedReader br = new BufferedReader(new FileReader(Params.dir+Params.networkFile));
            while ((line = br.readLine()) != null) {
                data = line.split("\t");
                if (data.length != 3)
                    continue;
                user1 = Integer.parseInt(data[0]);
                user2 = Integer.parseInt(data[1]);
                weight = Float.parseFloat(data[2]);
                if (!Params.numberOfNeighbours.containsKey(data[0]))
                    Params.numberOfNeighbours.put(data[0], 0);
                Params.numberOfNeighbours.put(data[0], Params.numberOfNeighbours.get(data[0])+1);
                if (!Params.numberOfNeighbours.containsKey(data[1]))
                    Params.numberOfNeighbours.put(data[1], 0);
                Params.numberOfNeighbours.put(data[1], Params.numberOfNeighbours.get(data[1])+1);
                if (weight < Params.threshold)
                    continue;
                Params.numberLinks++;
                UserLink<Integer,Float> link = new UserLink<Integer,Float>(user1, user2, weight);
                List<UserLink<Integer,Float>> nlist;
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
            System.out.println(Params.numberLinks+" links in the similarity network...");
            br.close();
        }
    }

    public static synchronized Network getInstance() {
        if(null == _instance){
            try {
                _instance = new Network();
            } catch (NumberFormatException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return _instance;
    }

    public Map<Integer,List<UserLink<Integer,Float>>> getNetwork(String net){
        return networks.get(net);
    }

    public String getNetworkTable(){
        return net_tab;
    }

}