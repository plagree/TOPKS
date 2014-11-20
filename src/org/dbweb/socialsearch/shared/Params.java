package org.dbweb.socialsearch.shared;

import java.util.HashMap;

public class Params {
	// ALL GLOBAL VARIABLES SHOULD BE HERE (config file)
	public static int[] seeker = {};
	public static String[] network = {"soc_snet_dt"};
	public static String dir = "";
	public static String taggers = "soc_tag_80";
	public static String networkFile = "user-to-user.txt";
	public static String ILFile = "tag-inverted.txt";
	public static String triplesFiles = "triples.txt";
	public static String tagFreqFile = "tag-freq.txt";
	public static String inputTestFile = "test-file.txt";
	public static String outputTestFile = "output-test-result.txt";
	public static HashMap<String, Integer> numberOfNeighbours;
	public static float threshold = 0.05f;
	public static int numberLinks = 0;
	public static int number_documents = 1570866;
	public static int number_users = 0;
}
