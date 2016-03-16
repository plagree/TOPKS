package org.dbweb.socialsearch.shared;

import java.util.Map;

public class Params {

  public static int[] seeker = {};
  public static String[] network = {"network"};
  public static String dir = "";
  public static String taggers = "soc_tag_80";
  public static String networkFile = "user-to-user.txt";
  public static String ILFile = "tag-inverted.txt";
  public static String triplesFile = "triples.txt";
  public static String inputTestFile = "test-file.txt";
  public static String outputTestFile = "output-test-result.txt";
  public static Map<String, Integer> numberOfNeighbours;
  public static float threshold = 0.05f;
  public static int numberLinks = 0;
  public static int number_documents = 1570866;
  public static int number_users = 0;
  public static int DISK_BUDGET = 0;
  public static int NUMBER_ILS = 0;
  public static int NORMALIZER = 150;
  public static boolean NDCG_TIME = false;
  public static boolean NDCG_USERS = false;
  public static int STEP_NEIGH = 20;
  public static long TIME_NDCG = 1;
  public static boolean EXACT_TOPK = false; // time to reach the exact TOPK_S
  public static boolean DISK_ACCESS_EXPERIMENT = false;
  public static int SIZE_OF_BLOCK = 512;
  public static boolean SUPERNODE = false;

  public static boolean DEBUG = false;
  public static boolean VERBOSE = false;
  public static int DUMB = 0;
}
