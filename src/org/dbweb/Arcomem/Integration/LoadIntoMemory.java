package org.dbweb.Arcomem.Integration;

import java.sql.Connection;

import org.dbweb.socialsearch.general.connection.PostgresqlConnection;
import org.dbweb.socialsearch.topktrust.algorithm.paths.Network;

public class LoadIntoMemory {
	/**
	 * Load data in main memory
	 *  - network
	 *  - user spaces (HashMap<user, HashMap< tag, HashSet<docs>>>)
	 *  - inverted lists
	 * Quite long, that's why it's done only once
	 */
	
	public static void loadData(Connection conn) {
		
		long beforeLoadData = System.currentTimeMillis();
		Network net = Network.getInstance(conn);
		long afterLoadData = System.currentTimeMillis();
		
		System.out.println("Data load in memory in "+(afterLoadData-beforeLoadData)+" ms...");
		
		return;
	}
}
