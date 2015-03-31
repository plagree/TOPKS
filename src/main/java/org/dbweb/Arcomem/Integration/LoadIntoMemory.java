package org.dbweb.Arcomem.Integration;

import java.sql.Connection;

import org.dbweb.socialsearch.topktrust.algorithm.paths.Network;

public class LoadIntoMemory {
	/**
	 * Load data in main memory
	 *  - network
	 *  - user spaces (HashMap<user, HashMap< tag, HashSet<docs>>>)
	 *  - inverted lists
	 * Quite long, that's why it's done only once
	 */
	
	private static long getUsedMemory() {
		return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	}
	
	public static void loadData(Connection conn) {
		final long start = getUsedMemory();
		long beforeLoadData = System.currentTimeMillis();
		Network net = Network.getInstance();
		long afterLoadData = System.currentTimeMillis();
		final long size = ( getUsedMemory() - start) / 1024 / 1024;
		System.out.println("Network file = " + size + "M");
		System.out.println("Data load in memory in "+(afterLoadData-beforeLoadData)+" ms...");
		
		return;
	}
}
