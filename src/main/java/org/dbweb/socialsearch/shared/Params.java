package org.dbweb.socialsearch.shared;

import java.util.Map;

public class Params {

  public static String[] network = {"network"};
  public static String dir = "";
  public static String networkFile = "user-to-user.txt";
  public static String ILFile = "tag-inverted.txt";
  public static String triplesFile = "triples.txt";
  public static Map<String, Integer> numberOfNeighbours;
  public static float threshold = 0.05f;
  public static int numberLinks = 0;
  public static int number_documents = 0;
  public static int number_users = 0;
  public static int DISK_BUDGET = 0;
  public static int NORMALIZER = 150;
  public static int STEP_NEIGH = 20;
  public static long TIME_NDCG = 1;
  public static int SIZE_OF_BLOCK = 512;

  public static boolean TEST = false;
  public static boolean DEBUG = false;
  public static boolean VERBOSE = false;
  public static boolean SIMRANK = false;
  public static boolean BASELINE = false;

}