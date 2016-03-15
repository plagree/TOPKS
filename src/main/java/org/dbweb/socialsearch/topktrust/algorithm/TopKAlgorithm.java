package org.dbweb.socialsearch.topktrust.algorithm;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.dbweb.socialsearch.shared.Params;
import org.dbweb.socialsearch.topktrust.algorithm.functions.PathCompositionFunction;
import org.dbweb.socialsearch.topktrust.algorithm.paths.OptimalPaths;
import org.dbweb.socialsearch.topktrust.algorithm.score.Score;
import org.dbweb.socialsearch.topktrust.datastructure.Item;
import org.dbweb.socialsearch.topktrust.datastructure.ItemList;
import org.dbweb.socialsearch.topktrust.datastructure.ReadingHead;
import org.dbweb.socialsearch.topktrust.datastructure.UserEntry;
import org.dbweb.completion.trie.RadixTreeImpl;
import org.dbweb.completion.trie.RadixTreeNode;
import org.externals.Tools.NDCG;
import org.externals.Tools.NDCGResults;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 *
 * @author Silviu & Paul
 */
public class TopKAlgorithm {

  protected ItemList candidates;				// Buffer of candidates
  protected TIntObjectMap<PatriciaTrie<TLongSet>> userSpaces;	// p-spaces
  protected TIntIntMap sizeUserSpaces;		// Useless?
  protected Set<String> invertedListsUsed;	// Statistics on ILs used
  protected Map<String, Integer> sizeInvertedLists;	// Useless?
  protected RadixTreeImpl tag_idf;			// Index of TF-IDFs
  protected List<Float> values;				// Useless
  protected Map<Integer, ReadingHead> topReadingHead;
  protected Map<String, Integer> invertedListPositions;
  protected Map<Integer, Float> userWeights;	// Social score (similarity) of the current visited user
  protected Map<String, Integer> tagFreqs;
  protected Set<Pair<Long,String>> unknown_tf;
  protected PatriciaTrie<String> dictionaryTrie;	// Trie containing all the words
  protected RadixTreeImpl completionTrie; 		// Completion trie
  protected List<String> correspondingCompletions;// TODO: What does it mean?
  protected Map<String, List<DocumentNumTag>> invertedLists;	// Index - inverted lists
  private Set<Long> guaranteed;
  private Set<Long> possible; // Set of items which are still candidates for the final topk
  protected float userWeight;
  protected UserEntry<Float> currentUser;
  protected PathCompositionFunction distFunc;
  private OptimalPaths optpath;
  private Score score;

  // NDCG lists
  protected List<Long> oracleNDCG;
  protected NDCGResults ndcgResults;
  // Time for exact top k
  protected long time_topk = 0;
  protected Set<Long> topk_infinity;

  //debug purpose
  public double bestscore;
  public List<Integer> visitedNodes;
  private int nbNeighbour;
  private List<Integer> queryNbNeighbour;
  protected boolean terminationCondition;
  protected long time_loop;
  protected int number_documents;
  protected int number_users;
  protected float alpha = 0;
  protected int nbPSpacesAccesses;// Accesses of p-spaces
  protected int nbILFastAccesses;
  protected int nbILAccesses;
  private int numloops = 0;
  private int skippedTests; 		// Number of loops before testing the exit condition
  private int maximumNodeVisited;	// Maximum number of users to visit
  private int numberUsersSeen;	// Current number of users seen


  /**
   * TODO
   * @param itemScore
   * @param scoreAlpha
   * @param distFunc
   * @param optPathClass
   */
  public TopKAlgorithm(Score itemScore, float scoreAlpha,
          PathCompositionFunction distFunc, OptimalPaths optPathClass) {
    this.distFunc = distFunc;
    this.alpha = scoreAlpha;
    this.optpath = optPathClass;
    this.score = itemScore;
    this.skippedTests = 10000;
    this.correspondingCompletions = null;
    long time_before_loading = System.currentTimeMillis();
    try {
      this.fileLoadingInMemory();
    } catch (IOException e) {
      e.printStackTrace();
    }
    long time_after_loading = System.currentTimeMillis();
    System.out.println("File loading in "+(float)(time_after_loading - time_before_loading)
            / 1000 +" sec...");
  }

  /**
   * Main call from TopKAlgorithm class, call this after building a new object
   * to run algorithm. This query method must be called for the first query.
   * When iterating to next letter, use method executeQueryNextLetter. When
   * starting a new word, use method executeQueryNextWord.
   * @param alpha   Parameter to balance textual and social contributions
   * @param seeker  Id of the seeker
   * @param query   List of keywords (last is considered as prefix)
   * @param k       Number of results in the top-k
   * @param t       Maximum time to compute the answer
   * @param maximumNodeVisited Maximum number of users to visit in the graph
   */
  public void executeQuery(int seeker, List<String> query, int k, float alpha,
          int t, int maximumNodeVisited) {

    // Step 0: Basic initialisations
    this.alpha = alpha;
    this.maximumNodeVisited = maximumNodeVisited;
    this.values = new ArrayList<Float>();		// TO REMOVE
    this.unknown_tf = new HashSet<Pair<Long,String>>();
    this.optpath.setValues(values);
    this.optpath.setDistFunc(distFunc);
    this.userWeight = 1.0f;
    this.terminationCondition = false;
    this.invertedListsUsed = new HashSet<String>();
    this.topReadingHead = new HashMap<Integer, ReadingHead>();
    this.userWeights = new HashMap<Integer, Float>();
    this.nbNeighbour = 0;
    this.queryNbNeighbour = new ArrayList<Integer>();
    this.candidates = new ItemList(this.score);	// New buffer because new query
    // Initialise the heap for Dijkstra
    this.currentUser = optpath.initiateHeapCalculation(seeker);
    // Initialise ChooseBranch heuristics
    this.candidates.setBranchHeuristics(query, this.completionTrie);
    for (int i = 0; i < query.size(); i++) {
      userWeights.put(i, this.userWeight);
      this.queryNbNeighbour.add(0);
    }

    // Step 1: Initialise index of prefix
    String prefix = query.get(query.size() - 1);
    // Get best index completion of the prefix
    String completion = completionTrie.searchPrefix(prefix, false)
            .getBestDescendant().getWord();
    this.invertedListsUsed.add(completion);
    // Get corresponding value and item id
    int value = (int)completionTrie.searchPrefix(prefix, false).getValue();
    long itemId = this.invertedLists.get(completion).get(0).getDocId();
    ReadingHead rh = new ReadingHead(completion, itemId, value);
    if (this.correspondingCompletions != null) {
      // rh.setCompletion(this.correspondingCompletions.get(0)); TODO
      return;
    }
    topReadingHead.put(query.size() - 1, rh);

    // Step 2: Initialise indices of words before prefix
    String keyword;
    for (int pos = 0; pos < query.size() - 1; pos++) {
      keyword = query.get(pos);
      this.invertedListsUsed.add(keyword);
      // Get corresponding value and item id
      value = this.invertedLists.get(keyword).get(0).getNum();
      itemId = this.invertedLists.get(keyword).get(0).getDocId();
      rh = new ReadingHead(keyword, itemId, value);
      topReadingHead.put(pos, rh);
    }

    // Step 4: Run the algorithm
    mainLoop(k, seeker, query, t);
  }

  /**
   * When a query with prefix was already answered, this method use previous
   * work and answer prefix + l
   * @param seeker Id of the seeker
   * @param query List of keywords (last is considered as prefix)
   * @param k Number of results in the top-k
   * @param t Maximum time to compute the answer
   */
  public void executeQueryNextLetter(
          int seeker, List<String> query, int k, int t) {
    // Update the unknown_tf
    int position = query.size() - 1; // Last word position index
    long timeBefore = System.currentTimeMillis();
    String newPrefix = query.get(query.size() - 1);
    RadixTreeNode radixTreeNode = completionTrie.searchPrefix(newPrefix, false);

    if (radixTreeNode == null)
      return;

    String bestCompletion = radixTreeNode.getBestDescendant().getWord();
    this.invertedListsUsed = new HashSet<String>();
    this.invertedListsUsed.add(bestCompletion);
    List<DocumentNumTag> arr = this.invertedLists.get(bestCompletion);

    if (this.invertedListPositions.get(bestCompletion) < arr.size()) {
      topReadingHead.put(
              position,
              new ReadingHead(bestCompletion, 
                      arr.get(this.invertedListPositions.get(bestCompletion)).getDocId(),
                      arr.get(this.invertedListPositions.get(bestCompletion)).getNum()));
    } else {
      topReadingHead.put(position, null);
    }

    this.candidates.filterNextLetter(query);

    long timeFiltering = System.currentTimeMillis() - timeBefore;
    //long timePrevious = this.time_topk;
    Params.DEBUG = false;

    if (this.topk_infinity.equals(this.candidates.getSetTopk(k))) {
      this.time_topk = timeFiltering;
      System.out.println("There was 0 loop ...");
    }
    else {
      mainLoop(k, seeker, query, t);
      this.time_topk += timeFiltering;
    }
  }

  /**
   * This method is used when we do an incremental version of the algorithm.
   * @param seeker Id of the seeker
   * @param query List of keywords (last is considered as prefix)
   * @param k Number of results in the top-k
   * @param t Maximum time to compute the answer
   */
  public void executeQueryNextWord(int seeker, List<String> query, int k, int t) {
    // We check that the new query has one more word than in previous execution
    if (this.topReadingHead.size() + 1 != query.size())
      System.out.println("executeQueryNextWord used with wrong number of params");
    // Initialise the heap for Dijkstra (start from origin again)
    this.currentUser = optpath.initiateHeapCalculation(seeker);
    this.userWeight = 1.0f;
    this.terminationCondition = false;
    this.nbNeighbour = 0;
    // Clean previous prefix for new word
    this.candidates.filterNextWord(query, this.tag_idf, this.completionTrie);

    // Add a new reading head for the new word
    String prefix = query.get(query.size() - 1);
    // Get best index completion of the prefix
    String completion = completionTrie.searchPrefix(prefix, false)
            .getBestDescendant().getWord();
    this.invertedListsUsed.add(completion);
    // Get corresponding value and item id
    int value = (int)completionTrie.searchPrefix(prefix, false).getValue();
    long itemId = this.invertedLists.get(completion).get(0).getDocId();
    ReadingHead rh = new ReadingHead(completion, itemId, value);
    if (this.correspondingCompletions != null) {
      return;
    }
    this.topReadingHead.put(query.size() - 1, rh);
    // TODO Update the unknown_tf map
    mainLoop(k, seeker, query, t);
  }


  /**
   * After answering a query session (initial prefix and possible completions),
   * we need to reinitialise the trie and go back to the initial position of
   * the tries.
   * @param query List of query keywords
   * @param length
   */
  public void reset(List<String> query, int length) {
    String prefix = "";
    for (String keyword: query) {
      prefix = keyword.substring(0, length);
      SortedMap<String, String> completions = this.dictionaryTrie
              .prefixMap(prefix);
      Iterator<Entry<String, String>> iterator = completions
              .entrySet().iterator();
      Entry<String, String> currentEntry = null;
      userWeight = 1;
      while (iterator.hasNext()) {
        currentEntry = iterator.next();
        String completion = currentEntry.getKey();

        if (!this.invertedListPositions.containsKey(completion))
          System.out.println(completion);
        if (this.invertedListPositions.get(completion) == 0)
          continue;

        this.invertedListPositions.put(completion, 0);
        DocumentNumTag firstDoc = this.invertedLists.get(completion).get(0);
        RadixTreeNode current_best_leaf = completionTrie
                .searchPrefix(completion, true).getBestDescendant();
        current_best_leaf.updatePreviousBestValue(firstDoc.getNum());
        current_best_leaf = completionTrie.searchPrefix("res", false)
                .getBestDescendant();
      }
    }
  }

  /**
   * Main loop of the TOPKS algorithm
   * @param k	Number of answers in the top-k
   * @param seeker Id of the user who issued the query
   * @param query List of the words in the query (last is a prefix)
   * @param max_t Maximum time to give a response
   */
  protected void mainLoop(int k, int seeker, List<String> query, int max_t) {
    // General attribute initialisations
    this.guaranteed = new HashSet<Long>();
    this.possible = new HashSet<Long>();
    this.numberUsersSeen = 0;
    this.ndcgResults = new NDCGResults();
    this.time_topk = 0;
    // Reset counter of IL accesses and p-spaces accesses
    this.nbILAccesses = 0;
    this.nbILFastAccesses = 0;
    this.nbPSpacesAccesses = 0;

    // Local Variables
    int loops = 0;
    int steps = 1;
    boolean underTimeLimit = true;
    long currentTime = 0;
    long time_NDCG = 0;
    long timeThresholdNDCG = Params.TIME_NDCG;
    int currVisited = 0;
    long before_main_loop = System.currentTimeMillis();
    if (Params.DEBUG == true)
      System.out.println("aaa");

    do {
      if (Params.DISK_ACCESS_EXPERIMENT && (currVisited + this.invertedListsUsed.size() +
              Params.NUMBER_ILS) >= Params.DISK_BUDGET)
        break;
      if (Params.DEBUG == true)
        System.out.println("bbbb");
      boolean socialBranch = chooseBranch(query);
      if (Params.DEBUG == true)
        System.out.println("ccc");
      if(socialBranch) {
        if (this.alpha == 1 || this.currentUser == null) {
          System.out.println("SHORT BREAK");
          break;
        }
        if (this.currentUser.getEntryId() != seeker)
          currVisited += 1;
        if (Params.DEBUG == true)
          System.out.println("a");
        processSocial(query, seeker);
        if (Params.DEBUG == true)
          System.out.println("b");
        lookIntoList(query);   //the "peek at list" procedure
        if (Params.DEBUG == true)
          System.out.println("c");
      } else {
        System.out.println("Process textual");
        processTextual(query);
      }

      steps = (steps + 1) % skippedTests;
      if (steps == 0) {
        terminationCondition = candidates.terminationCondition(
                query, k, this.alpha, this.tag_idf, this.topReadingHead,
                this.userWeights, this.possible);

        //long time_1 = System.currentTimeMillis();
        /*if ((time_1 - before_main_loop) > max_t) {  // TODO
                    this.candidates.extractProbableTopK(k, this.guaranteed, this.possible,
                            this.topReadingHead, this.userWeights,
                            this.invertedListPositions);
                    underTimeLimit = false;
                }*/
      } else { // Analysis for NDCG experiments
        // Curve NDCG vs time
        if (Params.DEBUG) {
          System.out.println(Params.EXACT_TOPK + ", " +
                  currVisited+", "+Params.NDCG_TIME+", "+Params.NDCG_USERS);
          System.out.println(Params.EXACT_TOPK && (currVisited%10 == 0));
        }
        if (Params.NDCG_TIME) {
          // Analysis for NDCG vs t plot
          currentTime = (System.currentTimeMillis() - before_main_loop) - time_NDCG / 1000000;
          if (Params.NDCG_TIME && (currentTime >= timeThresholdNDCG)) {
            long bef = System.nanoTime();
            double ndcg = NDCG.getNDCG(this.candidates.getLongListTopk(k), 
                    this.oracleNDCG, k);
            if (ndcg > 1) {
              System.out.println(ndcg);
              System.out.println(this.candidates.getLongListTopk(k).toString()
                      + ",   "+oracleNDCG.toString());
            }
            this.ndcgResults.addPoint(currentTime, ndcg);
            time_NDCG += (System.nanoTime() - bef);
            timeThresholdNDCG += Params.TIME_NDCG;
          }
          if (currentTime >= max_t && Params.NDCG_TIME) {
            System.out.println("Time: " + max_t);
            break;
          }
        }
        // Curve NDCG vs #users visited in the social graph
        else if (Params.NDCG_USERS) {
          if (currVisited%Params.STEP_NEIGH == 0) {
            double ndcg = NDCG.getNDCG(
                    this.candidates.getLongListTopk(k), oracleNDCG, k);
            this.ndcgResults.addPoint(currVisited, ndcg);
          }
        }
        else if (Params.EXACT_TOPK && (currVisited % 100 == 0)) {
          currentTime = (System.currentTimeMillis() - before_main_loop) - time_NDCG / 1000000;
          long bef = System.nanoTime();
          if (this.topk_infinity.equals(this.candidates.getSetTopk(k))) {
            this.time_topk = currentTime;
            break;
          }
          time_NDCG += (System.nanoTime() - bef);
        }
        terminationCondition = false;
        if (Params.DEBUG == true)
          System.out.println("e");
      } // End experiments
      long time_1 = System.currentTimeMillis();
      if ((time_1-before_main_loop) > Math.max(max_t + 25, max_t)) {
        System.out.println("time not under limit");
        underTimeLimit = false;
      }
      if (userWeight == 0) {
        terminationCondition = true;
      }
      loops++;
      if (currVisited >= this.maximumNodeVisited) {	// We visited enough users
        terminationCondition = true;
      }
      if (Params.DEBUG == true)
        System.out.println("f");
    } while(!terminationCondition && underTimeLimit);

    this.numloops = loops;
    this.numberUsersSeen = currVisited;
    System.out.println("There were " + loops + " loops ...");
  }

  /**
   * When alpha > 0, we need to alternate between Social Branch and Textual Branch
   * This method selects which branch will be chosen in the current loop
   * @param query
   * @return true (social) , false (textual)
   */
  protected boolean chooseBranch(List<String> query) {

    float upper_social_score;
    float upper_textual_score;
    boolean textual = false;

    if (this.topReadingHead.get(query.get(query.size() - 1)) == null)
      return !textual;

    for(int pos = 0; pos < query.size(); pos++) {
      if (userWeights.get(pos) == null) {
        System.out.println("WHY HERE? "+this.numloops);
        System.exit(0);
        return false; // (choose textual)
      }
      // OK but strange if skipped_tests != 0
      upper_social_score = (1 - this.alpha) * this.userWeights.get(pos)
              * this.candidates.getSocialBranchHeuristic(pos);
      upper_textual_score = this.alpha * this.candidates
              .getTextualBranchHeuristic(pos);

      if ((upper_social_score != 0) || (upper_textual_score != 0))
        textual = textual || (upper_social_score <= upper_textual_score);
    }
    return !textual;
  }


  /**
   * Social process of the TOPKS algorithm
   * @param query
   */
  protected void processSocial(List<String> query, int seeker) {
    // We check that haven't finished visiting the network yet
    if (this.currentUser == null) {
      this.userWeight = 0;
      for (int i = 0; i < query.size(); i++)
        userWeights.put(i, this.userWeight);
      return;
    }

    int currentUserId = 0, nbNeighbourTag = 0;
    long  itemId = 0;
    String tag;

    // for all tags in the query Q, triples Tagged(u,i,t_j)
    for(int pos = 0; pos < query.size(); pos++) {
      tag = query.get(pos);
      nbNeighbourTag = this.queryNbNeighbour.get(pos);
      if (nbNeighbourTag > this.nbNeighbour) {
        continue; // We don't need to analyse this word because it was already done previously
      }
      this.queryNbNeighbour.set(pos, this.nbNeighbour + 1);
      userWeights.put(pos, this.userWeight);
      currentUserId = currentUser.getEntryId();
      if (this.userSpaces.containsKey(currentUserId) &&
              (currentUserId != seeker || Params.SUPERNODE)) {
        // Here we check user space access
        this.nbPSpacesAccesses += 1;

        if (pos == query.size() - 1) { // Case 1: we are at a prefix
          SortedMap<String, TLongSet> completions = userSpaces
                  .get(currentUserId).prefixMap(tag);
          if (completions.size() > 0) {
            Iterator<Entry<String, TLongSet>> iterator = completions
                    .entrySet().iterator();
            // Iteration over every possible completion
            while (iterator.hasNext()) {
              Entry<String, TLongSet> currentEntry = iterator.next();
              String completion = currentEntry.getKey();
              for (TLongIterator it = currentEntry.getValue()
                      .iterator(); it.hasNext(); ) {
                itemId = it.next();
                // Add the item if not discovered yet
                if (!this.candidates.containsItemId(itemId)) {
                  this.candidates.addItem(itemId, this.alpha);
                  this.possible.add(itemId);
                }
                // Add the tag if not seen for this item yet
                if (!this.candidates.getItem(itemId).containsTag(completion)) {
                  float idf = tag_idf.searchPrefix(completion, true).getValue();
                  this.candidates.getItem(itemId).addTag(
                          completion, true, idf, pos);
                  // TF for (itemId, tag) is unknown
                  this.unknown_tf.add(new Pair<Long,String>(itemId, completion));
                }
                // Update the social score
                this.candidates.updateSocialScore(
                        itemId, completion, this.userWeight);        
              }
            }
          }
        } else { // Case 2: the word is not a prefix
          TLongSet taggedItems = this.userSpaces.get(currentUserId).get(tag);
          for (TLongIterator it = taggedItems.iterator(); it.hasNext(); ){
            itemId = it.next();
            // Add the item if not discovered yet
            if (!this.candidates.containsItemId(itemId)) {
              this.candidates.addItem(itemId, this.alpha);
              this.possible.add(itemId);
            }
            // Add the tag if not seen for this item yet
            if (!this.candidates.getItem(itemId).containsTag(tag)) {
              float idf = tag_idf.searchPrefix(tag, true).getValue();
              // Add new tag to the corresponding Item
              this.candidates.getItem(itemId).addTag(tag, false, idf, pos);
              // TF for (itemId, tag) is unknown
              this.unknown_tf.add(new Pair<Long,String>(itemId, tag));
            }
            // Update the social score
            this.candidates.updateSocialScore(itemId, tag, userWeight);
          }
        }
      }
    }

    this.nbNeighbour++;
    // Sometimes, this code runs really slowly TODO
    long time_loading_before = System.currentTimeMillis();
    currentUser = optpath.advanceFriendsList(currentUser);
    long time_loading_after = System.currentTimeMillis();
    long tl = (time_loading_after - time_loading_before) / 1000;
    if (tl > 1)
      System.out.println("Loading in : "+tl);
    if(currentUser != null)
      this.userWeight = currentUser.getDist().floatValue();
    else
      this.userWeight = 0;
  }

  /**
   * We advance on Inverted Lists here (method for social branch)
   * Given the new discovered items in User Spaces, do top-items can be updated?
   * @param query List<String>
   */
  private void lookIntoList(List<String> query) {
    boolean found;
    String keyword;
    ReadingHead currentReadingHead;
    for (int pos = 0; pos < query.size(); pos++) {
      found = true;
      while (found) {
        currentReadingHead = this.topReadingHead.get(pos);
        // Check if we reached the end of the inverted list
        if (currentReadingHead == null)
          break;
        found = false;
        keyword = currentReadingHead.getCompletion();
        if (unknown_tf.contains(currentReadingHead.getItemKeywordPair())) {
          found = true;	// The entry has been discovered in the graph
          if (this.candidates.containsItemId(currentReadingHead.getItemId())) {
            this.candidates.getItem(currentReadingHead.getItemId()).updateTDFScore(
                    keyword, currentReadingHead.getValue());
          }
          // We found the entry in IL
          this.unknown_tf.remove(currentReadingHead.getItemKeywordPair());
          if (pos == query.size() - 1)	//  prefix
            advanceTextualList(query.get(pos), pos, false);
          else
            advanceTextualList(query.get(pos), pos, true);
        }
      }
    }
  }

  /**
   * We chose the textual branch (alpha > 0), we advance in inverted lists
   * @param query List of words in the query
   */
  protected void processTextual(List<String> query) {
    ReadingHead currentEntry;
    long itemId;
    String keyword;
    for (int pos = 0; pos < query.size(); pos++) {
      // Check if current visited user is a new word for this word
      if (this.queryNbNeighbour.get(pos) > this.nbNeighbour) {
        continue;
      }
      // Check if there are still entries in the inverted list
      if (this.topReadingHead.get(pos) == null) {
        continue;
      }
      currentEntry = topReadingHead.get(pos);
      itemId = currentEntry.getItemId();
      keyword = currentEntry.getCompletion();
      // Add the item if it is not resent in the candidate list
      if (!this.candidates.containsItemId(itemId)) {
        this.candidates.addItem(itemId, this.alpha);
        this.possible.add(itemId);
      }
      // Add the tag if not seen for this item yet
      if (!this.candidates.getItem(itemId).containsTag(keyword)) {
        float idf = tag_idf.searchPrefix(keyword, true).getValue();
        // Add new tag to the corresponding Item
        if (pos == query.size() - 1)	// prefix
          this.candidates.getItem(itemId).addTag(keyword, true, idf, pos);
        else	// Not a prefix
          this.candidates.getItem(itemId).addTag(keyword, false, idf, pos);
      }
      // Update TF value
      this.candidates.getItem(itemId).updateTDFScore(keyword, currentEntry.getValue());
      // TF for (itemId, tag) is now known
      if (this.unknown_tf.contains(currentEntry.getItemKeywordPair()))
        this.unknown_tf.remove(currentEntry.getItemKeywordPair());
      if (pos == query.size() - 1)	// prefix, we don't search for exact match
        advanceTextualList(query.get(pos), pos, false);
      else
        advanceTextualList(query.get(pos), pos, true);
    }
  }

  /**
   * Method to advance in inverted lists (using the trie).
   * Used both in Social and Textual branches.
   * @param tag Word from the query
   * @param pos Position of the tag in the query
   * @param exact True if we want the exact word (not prefix), false otherwise (prefix)
   */
  protected void advanceTextualList(String tag, int pos, boolean exact) {
    RadixTreeNode current_best_leaf = completionTrie.searchPrefix(tag, exact).getBestDescendant();
    if (current_best_leaf.getWord().equals(tag)) {	// Statistics
      this.nbILFastAccesses += 1;
    } else {
      this.nbILAccesses += 1;
    }
    String keyword = current_best_leaf.getWord();
    this.invertedListsUsed.add(keyword);
    // Get inverted list of keyword
    List<DocumentNumTag> invertedList = this.invertedLists.get(keyword);
    // Read one position
    this.invertedListPositions.put(keyword, this.invertedListPositions.get(keyword) + 1);
    int ILposition = this.invertedListPositions.get(keyword);
    if (ILposition < invertedList.size())
      current_best_leaf.updatePreviousBestValue(invertedList.get(ILposition).getNum());
    else
      current_best_leaf.updatePreviousBestValue(0);
    current_best_leaf = this.completionTrie.searchPrefix(tag, exact).getBestDescendant();	// TODO: analyse
    // Check if we finished reading the entries of the IL
    if (current_best_leaf.getValue() == 0) {
      this.topReadingHead.put(pos, null);
      return;
    }
    keyword = current_best_leaf.getWord();
    ILposition = this.invertedListPositions.get(keyword);
    DocumentNumTag current_read = this.invertedLists.get(keyword).get(ILposition);
    ReadingHead new_top_rh = null;
    if (this.correspondingCompletions == null)
      new_top_rh = new ReadingHead(keyword, current_read.getDocId(), current_read.getNum());
    else	// TODO: Analyse when it happens
      new_top_rh = new ReadingHead(this.correspondingCompletions.get(ILposition),
              current_read.getDocId(), current_read.getNum());
    this.topReadingHead.put(pos, new_top_rh);
  }

  /**
   * Gives the ranking of a given item in the ranked list of discovered items
   * @param item
   * @param k
   * @return ranking (int)
   */
  public int getRankingItem(long item, int k) {
    return this.candidates.getRankingItem(item, k);
  }

  public Set<Long> getSetTopk(int k){
    return candidates.getSetTopk(k);
  }

  private static long getUsedMemory() {
    return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
  }

  /**
   * Method to load file with triples in memory
   * @throws IOException
   */
  private void fileLoadingInMemory() throws IOException {
    final long start = getUsedMemory();
    this.completionTrie = new RadixTreeImpl(); //
    // positions for a given keyword in the graph (useful for multiple words)
    this.invertedListPositions = new HashMap<String, Integer>(16, 0.85f);
    this.tagFreqs = new HashMap<String,Integer>(16, 0.85f); //DONE BUT NOT USED
    this.tag_idf = new RadixTreeImpl(); //DONE
    this.invertedLists = new HashMap<String, List<DocumentNumTag>>(16, 0.85f); //DONE
    this.userSpaces = new TIntObjectHashMap<PatriciaTrie<TLongSet>>(16, 0.85f);
    this.dictionaryTrie = new PatriciaTrie<String>(); // trie on the dictionary of words
    this.sizeUserSpaces = new TIntIntHashMap();
    this.sizeInvertedLists = new HashMap<String, Integer>();
    this.userWeight = 1.0f;

    BufferedReader br;
    String line;
    String[] data;

    if (Params.VERBOSE)
      System.out.println("Beginning of file loading...");

    // Tag Inverted lists processing
    br = new BufferedReader(new FileReader(Params.dir+Params.ILFile));
    List<DocumentNumTag> currIL;
    int counter = 0;
    while ((line = br.readLine()) != null) {
      data = line.split("\t");
      if (data.length < 2)
        continue;
      String tag = data[0];
      if (!this.invertedLists.containsKey(data[0]))
        this.invertedLists.put(tag, new ArrayList<DocumentNumTag>());
      currIL = this.invertedLists.get(data[0]);
      for (int i=1; i<data.length; i++) {
        String[] tuple = data[i].split(":");
        if (tuple.length != 2)
          continue;
        currIL.add(new DocumentNumTag(Long.parseLong(tuple[0]),
                Integer.parseInt(tuple[1])));
      }
      Collections.sort(currIL, Collections.reverseOrder());
      DocumentNumTag firstDoc = currIL.get(0);
      this.completionTrie.insert(tag, firstDoc.getNum());
      this.invertedListPositions.put(tag, 0);
      this.tagFreqs.put(tag, firstDoc.getNum());
      counter++;
      if ((counter%50000) == 0 && Params.VERBOSE)
        System.out.println("\t"+counter+" tag ILs loaded");
    }
    br.close();
    final long size = ( getUsedMemory() - start) / 1024 / 1024;
    if (Params.VERBOSE) {
      System.out.println("Inverted List file = " + size + "M");
      System.out.println("Inverted List file loaded...");
    }

    // Triples processing
    int userId;
    long itemId;
    String tag;
    final long start2 = getUsedMemory();
    br = new BufferedReader(new FileReader(Params.dir+Params.triplesFile));
    counter = 0;

    if (Params.VERBOSE)
      System.out.println("Loading of triples");

    while ((line = br.readLine()) != null) {
      data = line.split("\t");

      if (data.length != 3)
        continue;
      userId = Integer.parseInt(data[0]);
      itemId = Long.parseLong(data[1]);
      tag = data[2];
      if (!dictionaryTrie.containsKey(tag))
        this.dictionaryTrie.put(tag, ""); // This trie has no value
      if(!this.userSpaces.containsKey(userId)){
        this.userSpaces.put(userId, new PatriciaTrie<TLongSet>());
      }
      if(!this.userSpaces.get(userId).containsKey(tag))
        this.userSpaces.get(userId).put(tag, new TLongHashSet());
      this.userSpaces.get(userId).get(tag).add(itemId);
      counter++;
      if ((counter % 1000000) == 0 && Params.VERBOSE)
        System.out.println("\t" + counter + " triples loaded");
    }
    br.close();
    final long size2 = (getUsedMemory() - start2) / 1024 / 1024;

    // Computation of the space of user spaces and inverted lists
    TIntObjectIterator<PatriciaTrie<TLongSet>> it = this.userSpaces.iterator();
    while (it.hasNext()) { // Loop on userSpaces
      it.advance();
      int user = it.key();
      PatriciaTrie<TLongSet> patricia = this.userSpaces.get(user);
      Set<String> keys = patricia.keySet();
      int current_size = 0;
      for (String key: keys)
        current_size += key.length() + 1;
      Iterator<TLongSet> it2 = patricia.values().iterator();
      while (it2.hasNext()) {
        current_size += it2.next().size() * 8;
      }
      this.sizeUserSpaces.put(user, current_size);
    }
    Set<String> keys = this.invertedLists.keySet();
    for (String key: keys) {
      this.sizeInvertedLists.put(key, this.invertedLists.get(key).size()*8);
    }

    if (Params.VERBOSE)
      System.out.println("User spaces file = " + size2 + "M");
    Params.number_users = this.userSpaces.size();

    // Tag Freq processing
    final long start3 = getUsedMemory();
    br = new BufferedReader(new FileReader(Params.dir+Params.tagFreqFile));
    int tagfreq;
    while ((line = br.readLine()) != null) {
      data = line.split("\t");
      if (data.length != 2)
        continue;
      tag = data[0];
      tagfreq = Integer.parseInt(data[1]);
      //float tagidf = (float) Math.log(((float)Params.number_documents - (float)tagfreq + 0.5)/((float)tagfreq+0.5));
      float tagidf = (float) Math.log(0.5 + (float)Params.number_documents / ((float) tagfreq) ); // Old tf-idf
      tag_idf.insert(tag, tagidf);
    }
    br.close();
    final long size3 = (getUsedMemory() - start3) / 1024 / 1024;

    if (Params.VERBOSE)
      System.out.println("TagFreq file = " + size3 + "M");
  }

  public void setSkippedTests(int skippedTests) {
    this.skippedTests = skippedTests;
  }

  public void computeOracleNDCG(int k) {
    this.oracleNDCG = this.candidates.getLongListTopk(k);
  }

  public List<Long> getOracle() {
    return this.oracleNDCG;
  }

  public List<Item> getTopk(int k) {
    return this.candidates.getListTopk(k);
  }

  public void computeTopkInfinity(int k) {
    this.topk_infinity = this.candidates.getSetTopk(k);
  }

  public void setTopkInfinity(Set<Long> oracle) {
    this.topk_infinity = oracle;
  }

  public Set<Long> getTopkInfinity() {
    return this.topk_infinity;
  }

  public JsonObject getJsonNDCG_vs_time(int k) {
    JsonObject jsonResult = new JsonObject();
    JsonArray arrayResults = new JsonArray();
    JsonObject currItem;

    List<Double> ndcgs = this.ndcgResults.getNdcgs();
    List<Long> times = this.ndcgResults.getSteps();
    for (int i=0; i<this.ndcgResults.size(); i++) {
      currItem = new JsonObject();
      currItem.add("t", new JsonPrimitive(times.get(i)));					// time spent
      currItem.add("ndcg", new JsonPrimitive(ndcgs.get(i)));				// ndcg score
      arrayResults.add(currItem);
    }

    jsonResult.add("status", new JsonPrimitive(1)); 						// No problem appeared in TOPKS
    jsonResult.add("results", arrayResults);

    return jsonResult;
  }

  public JsonObject getJsonNDCG_vs_nbusers(int k) {
    JsonObject jsonResult = new JsonObject();
    JsonArray arrayResults = new JsonArray();
    JsonObject currItem;

    List<Double> ndcgs = this.ndcgResults.getNdcgs();
    List<Long> nbusers = this.ndcgResults.getSteps();
    for (int i=0; i<this.ndcgResults.size(); i++) {
      currItem = new JsonObject();
      currItem.add("nb_users", new JsonPrimitive(nbusers.get(i)));					// time spent
      currItem.add("ndcg", new JsonPrimitive(ndcgs.get(i)));				// ndcg score
      arrayResults.add(currItem);
    }

    jsonResult.add("status", new JsonPrimitive(1)); 						// No problem appeared in TOPKS
    jsonResult.add("results", arrayResults);

    return jsonResult;
  }

  public JsonObject getJsonExactTopK_vs_t(int k) {
    JsonObject jsonResult = new JsonObject();
    jsonResult.add("status", new JsonPrimitive(1)); 						// No problem appeared in TOPKS
    jsonResult.add("time", new JsonPrimitive(this.time_topk));

    return jsonResult;
  }

  public long getTimeTopK() {
    return this.time_topk;
  }

  public ItemList getCandidates() {
    return this.candidates;
  }

  public int getNumloops() {
    return this.numloops;
  }

  public int getNumberInvertedListUsed() {
    return this.invertedListsUsed.size();
  }

  public int getNumberUsersSeen() {
    return this.numberUsersSeen;
  }

  public int getPSpaceAccesses() {
    return this.nbPSpacesAccesses;
  }

  public JsonObject getILaccesses() {
    JsonObject accesses = new JsonObject();
    accesses.add("fast", new JsonPrimitive(this.nbILFastAccesses));
    accesses.add("slow", new JsonPrimitive(this.nbILAccesses));
    accesses.add("il_topks_asyt",  new JsonPrimitive(this.invertedListsUsed.size()));
    return accesses;
  }

  public double computeNDCG(int k) {
    return NDCG.getNDCG(this.candidates.getLongListTopk(k),
            this.oracleNDCG, k); // Compute NDCG with oracle list
  }

}