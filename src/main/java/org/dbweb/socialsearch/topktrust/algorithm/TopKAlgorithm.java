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
import org.dbweb.topktrust.socialsearch.importer.CSVFileImporter;
import org.dbweb.Arcomem.Integration.Baseline;
import org.dbweb.Arcomem.Integration.Experiment;
import org.dbweb.completion.trie.RadixTreeImpl;
import org.dbweb.completion.trie.RadixTreeNode;
import org.externals.Tools.ItemBaseline;
import org.externals.Tools.NDCG;
import org.externals.Tools.NDCGResults;

import gnu.trove.iterator.TLongIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TLongSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;

/**
 *
 * @author Silviu & Paul
 */
public class TopKAlgorithm {

  /* 1. Index of the considered dataset */
  private Map<String, Integer> invertedListPositions = new HashMap<String, Integer>();
  // TF-IDF index
  private RadixTreeImpl tagIdf = new RadixTreeImpl();
  // P-spaces
  private TIntObjectMap<PatriciaTrie<TLongSet>> userSpaces =
          new TIntObjectHashMap<PatriciaTrie<TLongSet>>();
  // Trie on the dictionary of words (contains all words)
  private PatriciaTrie<String> dictionaryTrie = new PatriciaTrie<String>();
  // Completion trie
  private RadixTreeImpl completionTrie = new RadixTreeImpl();
  // Index - inverted lists
  private Map<String, List<DocumentNumTag>> invertedLists = new HashMap<String,
          List<DocumentNumTag>>();
  private OptimalPaths optpath;

  /* 2. Session-dependent structures */
  private ItemList candidates;			// Buffer of candidates
  private Set<String> invertedListsUsed;// Statistics on ILs used
  private List<Float> values;
  private List<ReadingHead> topReadingHead;
  private List<Float> userWeights;  // Social score (similarity) of the current visited user
  private Set<Pair<Long,String>> unknownTf;
  private Set<Long> guaranteed;
  private Set<Long> possible; // Set of items which are still candidates for the final topk
  private float userWeight;
  private UserEntry<Float> currentUser;
  private PathCompositionFunction<Float> distFunc;
  private Score score;
  private Set<String> plainTerms; // Set of complete words (each word except prefix)

  // NDCG lists
  private List<Long> oracleNDCG;
  private NDCGResults ndcgResults;
  // Time for exact top k
  private long time_topk = 0;
  private Set<Long> topk_infinity;
  private Experiment type;

  //debug purpose
  public double bestscore;
  public List<Integer> visitedNodes;
  private int nbNeighbour;
  private List<Integer> queryNbNeighbour;
  private boolean terminationCondition;
  private float alpha = 0;
  private int nbPSpacesAccesses; // Accesses of p-spaces
  private int nbILFastAccesses; // Do we really care? TODO
  private int nbILAccesses;
  private int numloops = 0;
  private int skippedTests = 1;  // Number of loops before testing the exit condition
  private int maximumNodeVisited;// Maximum number of users to visit
  private int numberUsersSeen;	 // Current number of users seen


  /**
   * Constructor
   * @param itemScore
   * @param scoreAlpha
   * @param distFunc
   * @param optPathClass
   */
  public TopKAlgorithm(Score itemScore, float scoreAlpha,
          PathCompositionFunction<Float> distFunc, OptimalPaths optPathClass) {
    this.distFunc = distFunc;
    this.alpha = scoreAlpha;
    this.optpath = optPathClass;
    this.score = itemScore;
    this.type = Experiment.DEFAULT;
    long before = System.currentTimeMillis();
    try {
      CSVFileImporter.loadInMemory(this.completionTrie, this.tagIdf,
              this.invertedListPositions, this.invertedLists,
              this.dictionaryTrie, this.userSpaces);
    } catch (IOException e) {
      e.printStackTrace();
    }
    long after = System.currentTimeMillis();
    System.out.println("File loading: " + (float)(after - before) / 1000 + "s");
  }

  /**
   * Main call from TopKAlgorithm class, call this after building a new object
   * to run algorithm. This query method must be called for the first query.
   * When iterating to next letter, use method executeQueryNextLetter. When
   * starting a new word, use method executeQueryNextWord.
   * 
   * @param alpha   Parameter to balance textual and social contributions
   * @param seeker  Id of the seeker
   * @param query   List of keywords (last is considered as prefix)
   * @param k       Number of results in the top-k
   * @param t       Maximum time to compute the answer
   * @param maximumNodeVisited Maximum number of users to visit in the graph
   */
  public void executeQuery(int seeker, List<String> query, int k, float alpha,
          int t, int maximumNodeVisited, Experiment type) {

    // Step 0: Basic initialisations
    this.type = type;
    this.alpha = alpha;
    this.maximumNodeVisited = maximumNodeVisited;
    this.values = new ArrayList<Float>();
    this.unknownTf = new HashSet<Pair<Long,String>>();
    this.optpath.setValues(values);
    this.optpath.setDistFunc(distFunc);
    this.userWeight = 1.0f;
    this.invertedListsUsed = new HashSet<String>();
    this.topReadingHead = new ArrayList<ReadingHead>();
    this.userWeights = new ArrayList<Float>();
    this.nbNeighbour = 0;
    this.queryNbNeighbour = new ArrayList<Integer>();
    this.plainTerms = new HashSet<String>();
    // New buffer because new query
    this.candidates = new ItemList(this.score);
    // Initialise the heap for Dijkstra
    this.currentUser = optpath.initiateHeapCalculation(seeker);
    // Initialise ChooseBranch heuristics
    this.candidates.setBranchHeuristics(query, this.completionTrie);
    for (int i = 0; i < query.size(); i++) {
      this.userWeights.add(this.userWeight);
      this.queryNbNeighbour.add(0);
    }

    int value;
    long itemId;
    ReadingHead rh;

    // Step 1: Initialise indices of words before prefix
    String keyword;
    for (int pos = 0; pos < query.size() - 1; pos++) {
      keyword = query.get(pos);
      this.invertedListsUsed.add(keyword);
      // Get corresponding value and item id
      value = this.invertedLists.get(keyword).get(0).getNum();
      itemId = this.invertedLists.get(keyword).get(0).getDocId();
      rh = new ReadingHead(keyword, itemId, value);
      this.topReadingHead.add(rh);
      this.plainTerms.add(query.get(pos));
    }

    // Step 2: Initialise index of prefix
    String prefix = query.get(query.size() - 1);
    // Get best index completion of the prefix
    String completion = this.completionTrie.searchPrefix(prefix, false)
            .getBestDescendant().getWord();
    this.invertedListsUsed.add(completion);
    // Get corresponding value and item id
    value = (int)completionTrie.searchPrefix(prefix, false).getValue();
    itemId = this.invertedLists.get(completion).get(0).getDocId();
    rh = new ReadingHead(completion, itemId, value);
    this.topReadingHead.add(rh);
    if (this.plainTerms.contains(completion))
      this.advanceTextualList(prefix, query.size() - 1, false);

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
      topReadingHead.set(
              position,
              new ReadingHead(bestCompletion, 
                      arr.get(this.invertedListPositions.get(bestCompletion)).getDocId(),
                      arr.get(this.invertedListPositions.get(bestCompletion)).getNum()));
    } else {
      topReadingHead.set(position, null);
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
    this.nbNeighbour = 0;
    // Clean previous prefix for new word
    this.candidates.filterNextWord(query, this.tagIdf, this.completionTrie,
            this.unknownTf);

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
    this.userWeights.add(this.userWeight);
    this.topReadingHead.set(query.size() - 1, rh);
    this.plainTerms.add(query.get(query.size() - 2));
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
      while (iterator.hasNext()) {
        currentEntry = iterator.next();
        String completion = currentEntry.getKey();

        if (!this.invertedListPositions.containsKey(completion)) {
          System.out.println(completion);
          System.out.println("Erreur l319.");
        }
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
  private void mainLoop(int k, int seeker, List<String> query, int max_t) {
    // General attribute initialisations
    this.guaranteed = new HashSet<Long>();
    this.possible = new HashSet<Long>();
    this.numberUsersSeen = 0;
    this.ndcgResults = new NDCGResults();
    this.time_topk = 0;
    this.terminationCondition = false;
    // Reset counter of IL accesses and p-spaces accesses
    this.nbILAccesses = 0;
    this.nbILFastAccesses = 0;
    this.nbPSpacesAccesses = 0;

    // Local Variables
    int loops = 0;
    int steps = 1;
    long currentTime = 0;
    long timeNDCG = 0;
    long timeThresholdNDCG = Params.TIME_NDCG;
    int currVisited = 0;
    long before_main_loop = System.currentTimeMillis();

    do {
      if (this.type == Experiment.NDCG_DISK_ACCESS && (currVisited
              + this.invertedListsUsed.size()) >= Params.DISK_BUDGET)
        break;
      boolean socialBranch = chooseBranch(query);
      if(socialBranch) {
        if (this.alpha == 1 || this.currentUser == null) {
          break;
        }
        if (this.currentUser.getEntryId() != seeker)
          currVisited += 1;
        processSocial(query, seeker);
        lookIntoList(query);   //the "peek at list" procedure
      } else {
        processTextual(query);
      }

      steps = (steps + 1) % this.skippedTests;
      if (steps == 0) {
        this.terminationCondition = this.candidates.terminationCondition(
                query, k, this.alpha, this.tagIdf, this.topReadingHead,
                this.userWeights, this.possible);

      } else { // Experiments
        switch (this.type) {
          case NDCG_TIME: // Analysis for NDCG vs t plot
            currentTime = (System.currentTimeMillis() - before_main_loop)
            - timeNDCG / 1000000;
            if (currentTime >= timeThresholdNDCG) {
              long bef = System.nanoTime();
              double ndcg = NDCG.getNDCG(this.candidates.getLongListTopk(k), 
                      this.oracleNDCG, k);
              this.ndcgResults.addPoint(currentTime, ndcg);
              timeNDCG += (System.nanoTime() - bef);
              timeThresholdNDCG += Params.TIME_NDCG;
            }
            if (currentTime >= max_t) {
              System.out.println("Time: " + max_t);
              break;
            }
            break;
          case NDCG_USERS: // Curve NDCG vs #users visited in the social graph
            if (currVisited%Params.STEP_NEIGH == 0) {
              double ndcg = NDCG.getNDCG(
                      this.candidates.getLongListTopk(k), oracleNDCG, k);
              this.ndcgResults.addPoint(currVisited, ndcg);
            }
            break;
          case EXACT_TOPK:
            if (currVisited % 100 == 0) {
              currentTime = (System.currentTimeMillis() - before_main_loop)
                      - timeNDCG / 1000000;
              long bef = System.nanoTime();
              if (this.topk_infinity.equals(this.candidates.getSetTopk(k))) {
                this.time_topk = currentTime;
                break;
              }
              timeNDCG += (System.nanoTime() - bef);
            }
            break;
          default: ;
        }
        terminationCondition = false;
      } // End experiments

      loops++;
      long time_1 = System.currentTimeMillis();
      if ((time_1-before_main_loop) > Math.max(max_t + 25, max_t)) {
        System.out.println("time not under limit");
        break;
      }
      if (userWeight == 0 || currVisited >= this.maximumNodeVisited) {
        break;
      }
    } while(!this.terminationCondition);

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
  private boolean chooseBranch(List<String> query) {

    float upper_social_score;
    float upper_textual_score;
    boolean textual = false;

    if (this.topReadingHead.get(query.size() - 1) == null)
      return !textual;

    for(int pos = 0; pos < query.size(); pos++) {
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
   * Social process of the TOPKS algorithm. Note: The algorithm does not allow
   * a completion to be equal to a previous term in the query.
   * @param query
   */
  private void processSocial(List<String> query, int seeker) {
    // We check that haven't finished visiting the network yet
    if (this.currentUser == null) {
      this.userWeight = 0;
      for (int i = 0; i < query.size(); i++)
        this.userWeights.set(i, this.userWeight);
      return;
    }

    int currentUserId = 0, nbNeighbourTag = 0;
    long  itemId = 0;
    String tag;
    boolean badCompletion; // True if the completion corresponds to a previous query term
    boolean pspaceExploration = false; // Did we explore the p-space of the current user?

    // for all tags in the query Q, triples Tagged(u,i,t_j)
    for(int pos = 0; pos < query.size(); pos++) {
      tag = query.get(pos);
      nbNeighbourTag = this.queryNbNeighbour.get(pos);
      if (nbNeighbourTag > this.nbNeighbour) {
        continue; // We don't need to analyse this word because it was already done previously
      }
      this.queryNbNeighbour.set(pos, this.nbNeighbour + 1);
      this.userWeights.set(pos, this.userWeight);
      currentUserId = currentUser.getEntryId();
      if (this.userSpaces.containsKey(currentUserId) && (currentUserId != seeker
              || this.type == Experiment.SUPERNODE)) {
        pspaceExploration = true;
        if (pos == query.size() - 1) { // Case 1: we are at a prefix
          SortedMap<String, TLongSet> completions = this.userSpaces
                  .get(currentUserId).prefixMap(tag);
          if (completions.size() > 0) {
            Iterator<Entry<String, TLongSet>> iterator = completions
                    .entrySet().iterator();
            // Iteration over every possible completion
            while (iterator.hasNext()) {
              badCompletion = false;
              Entry<String, TLongSet> currentEntry = iterator.next();
              String completion = currentEntry.getKey();
              for (int i = 0; i < query.size() - 1; i++) {
                if (completion.equals(query.get(i)))
                  badCompletion = true;
              }
              if (badCompletion)
                continue;
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
                  float idf = this.tagIdf.searchPrefix(
                          completion, true).getValue();
                  this.candidates.addTagToItem(itemId, completion, true, idf, pos);
                  // TF for (itemId, tag) is unknown
                  this.unknownTf.add(new Pair<Long,String>(itemId, completion));
                }
                // Update the social score
                this.candidates.updateSocialScore(
                        itemId, completion, this.userWeight);        
              }
            }
          }
        } else { // Case 2: the word is not a prefix
          TLongSet taggedItems = this.userSpaces.get(currentUserId).get(tag);
          if (taggedItems != null) { // The user tagged items with this tag
            for (TLongIterator it = taggedItems.iterator(); it.hasNext(); ) {
              itemId = it.next();
              // Add the item if not discovered yet
              if (!this.candidates.containsItemId(itemId)) {
                this.candidates.addItem(itemId, this.alpha);
                this.possible.add(itemId);
              }
              // Add the tag if not seen for this item yet
              if (!this.candidates.getItem(itemId).containsTag(tag)) {
                float idf = this.tagIdf.searchPrefix(tag, true).getValue();
                // Add new tag to the corresponding Item
                // this.candidates.getItem(itemId).addTag(tag, false, idf, pos);
                this.candidates.addTagToItem(itemId, tag, false, idf, pos);
                // TF for (itemId, tag) is unknown
                this.unknownTf.add(new Pair<Long, String>(itemId, tag));
              }
              // Update the social score
              this.candidates.updateSocialScore(itemId, tag, userWeight);
            }
          }
        }
      }
    }

    if (pspaceExploration) // Here we check user space access
      this.nbPSpacesAccesses += 1;
    this.nbNeighbour++;
    currentUser = optpath.advanceFriendsList(currentUser); // Go to next user
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
        if (this.unknownTf.contains(currentReadingHead.getItemKeywordPair())) {
          found = true;	// The entry has been discovered in the graph
          if (this.candidates.containsItemId(currentReadingHead.getItemId())) {
            //this.candidates.getItem(currentReadingHead.getItemId()).updateTDFScore(
            //        keyword, currentReadingHead.getValue());
            this.candidates.updateTextualScore(currentReadingHead.getItemId(),
                    keyword, currentReadingHead.getValue());
          }
          // We found the entry in IL
          this.unknownTf.remove(currentReadingHead.getItemKeywordPair());
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
  private void processTextual(List<String> query) {
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
        float idf = this.tagIdf.searchPrefix(keyword, true).getValue();
        // Add new tag to the corresponding Item
        if (pos == query.size() - 1) { // prefix
          //this.candidates.getItem(itemId).addTag(keyword, true, idf, pos);
          this.candidates.addTagToItem(itemId, keyword, true, idf, pos);
        }
        else {	// Not a prefix
          //this.candidates.getItem(itemId).addTag(keyword, false, idf, pos);
          this.candidates.addTagToItem(itemId, keyword, true, idf, pos);
        }
      }
      // Update TF value
      this.candidates.updateTextualScore(itemId, keyword, currentEntry.getValue());
      // TF for (itemId, tag) is now known
      if (this.unknownTf.contains(currentEntry.getItemKeywordPair()))
        this.unknownTf.remove(currentEntry.getItemKeywordPair());
      if (pos == query.size() - 1)	// prefix, we don't search for exact match
        advanceTextualList(query.get(pos), pos, false);
      else
        advanceTextualList(query.get(pos), pos, true);
    }
  }

  /**
   * Method to advance in inverted lists (using the trie). Used both in Social
   * and Textual branches. Note: two identical terms in the query are counted
   * only once.
   * @param tag Word from the query
   * @param pos Position of the tag in the query
   * @param exact True if we want the exact word (not prefix), false otherwise
   *  (prefix)
   */
  private void advanceTextualList(String tag, int pos, boolean exact) {
    if (exact) { // Not a prefix, read directly in inverted lists
      this.nbILFastAccesses += 1;
      this.invertedListsUsed.add(tag);
      // Get inverted list of keyword
      List<DocumentNumTag> invertedList = this.invertedLists.get(tag);
      // Read one position
      this.invertedListPositions.put(tag, this.invertedListPositions
              .get(tag) + 1);
      int ILposition = this.invertedListPositions.get(tag);
      if (ILposition == invertedList.size()) {
        this.topReadingHead.set(pos, null);
        return;
      }
      ILposition = this.invertedListPositions.get(tag);
      DocumentNumTag current_read = this.invertedLists.get(tag).get(ILposition);
      ReadingHead new_top_rh = null;
      new_top_rh = new ReadingHead(
              tag, current_read.getDocId(), current_read.getNum());
      this.topReadingHead.set(pos, new_top_rh);
    }
    else { // Prefix, use the completion trie
      RadixTreeNode current_best_leaf = completionTrie.searchPrefix(tag, exact)
              .getBestDescendant();
      if (current_best_leaf.getWord().equals(tag)) {  // Statistics
        this.nbILFastAccesses += 1;
      } else {
        this.nbILAccesses += 1;
      }
      String keyword;
      int ILposition;
      do {
        keyword = current_best_leaf.getWord();
        if (this.plainTerms.contains(keyword)) {
          current_best_leaf.updatePreviousBestValue(0);
        } else {
          this.invertedListsUsed.add(keyword);
          // Get inverted list of keyword
          List<DocumentNumTag> invertedList = this.invertedLists.get(keyword);
          // Read one position
          this.invertedListPositions.put(keyword, this.invertedListPositions
                  .get(keyword) + 1);
          ILposition = this.invertedListPositions.get(keyword);
          if (ILposition < invertedList.size())
            current_best_leaf.updatePreviousBestValue(
                    invertedList.get(ILposition).getNum());
          else
            current_best_leaf.updatePreviousBestValue(0);
        }
        current_best_leaf = this.completionTrie.searchPrefix(tag, exact)
                .getBestDescendant();
        // Check if we finished reading the entries of the IL
        if (current_best_leaf.getValue() == 0) {
          this.topReadingHead.set(pos, null);
          return;
        }
        keyword = current_best_leaf.getWord();
        if (!this.plainTerms.contains(keyword))
          break;
      } while (true);
      ILposition = this.invertedListPositions.get(keyword);
      DocumentNumTag current_read = this.invertedLists.get(keyword).get(ILposition);
      ReadingHead new_top_rh = null;
      new_top_rh = new ReadingHead(
              keyword, current_read.getDocId(), current_read.getNum());
      this.topReadingHead.set(pos, new_top_rh);
    }
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

  /*public JsonObject getILaccesses() {
    JsonObject accesses = new JsonObject();
    accesses.add("fast", new JsonPrimitive(this.nbILFastAccesses));
    accesses.add("slow", new JsonPrimitive(this.nbILAccesses));
    accesses.add("il_topks_asyt",  new JsonPrimitive(this.invertedListsUsed.size()));
    return accesses;
  }*/

  /**
   * Compute NDCG with oracle list
   * @param k
   * @return NDCG measure between last top-k and oracle.
   */
  public double computeNDCG(int k) {
    double v = NDCG.getNDCG(this.candidates.getLongListTopk(k),
            this.oracleNDCG, k);
    if (v > 1) {
      this.candidates.debug(k);
    }
    return v;
  }

  /**
   * This method returns the NDCG of a baseline specified in parameter with
   * respect to the infinite algorithm.
   * 
   * @param seeker
   * @param query
   * @param k
   * @param alpha
   * @param t
   * @param nVisited
   * @param baseline
   * @return
   */
  public float executeJournalBaselineQuery(int seeker, List<String> query,
          int k, float alpha, int t, int nVisited, Baseline baseline) {

    if (baseline == Baseline.TEXTUAL_SOCIAL) {
      // Step 1: Fully textual
      this.skippedTests = 100000;
      this.executeQuery(seeker, query, k, 1, t, nVisited,
              Experiment.NDCG_DISK_ACCESS);

      // Step 2: Fully social now
      this.alpha = alpha;
      this.values = new ArrayList<Float>();
      this.optpath.setValues(values);
      this.optpath.setDistFunc(distFunc);
      this.userWeight = 1.0f;
      this.userWeights = new ArrayList<Float>();
      this.nbNeighbour = 0;
      this.queryNbNeighbour = new ArrayList<Integer>();
      // Initialise the heap for Dijkstra
      this.currentUser = optpath.initiateHeapCalculation(seeker);
      for (int i = 0; i < query.size(); i++) {
        this.userWeights.add(this.userWeight);
        this.queryNbNeighbour.add(0);
      }
      this.candidates.updateAlpha(alpha);
      this.mainLoop(k, seeker, query, 2000);
      return (float)this.computeNDCG(k);
    }
    else if (baseline == Baseline.TOPK_MERGE) {
      // Step 1: fully textual
      this.skippedTests = 1;
      this.executeQuery(seeker, query, k, 1, t, nVisited,
              Experiment.DEFAULT);
      List<Item> topkTextual = this.candidates.getListTopk(k);
      System.out.println("TEXTUAL");
      int i = 0;
      for (Item e: this.candidates.getListTopk(k + 3)) {
        if (i < (k + 5))
          System.out.println(e);
        i++;
      }
      this.reset(query, 1);
      // Step 2: fully social
      this.executeQuery(seeker, query, k, 0, t, nVisited,
              Experiment.DEFAULT);
      System.out.println("SOCIAL");
      i = 0;
      for (Item e: this.candidates.getListTopk(k + 3)) {
        if (i < (k + 5))
          System.out.println(e);
        i++;
      }
      List<Item> topkSocial = this.candidates.getListTopk(k);
      this.reset(query, 1);
      // Merge lists
      Map<Long, ItemBaseline> items = new HashMap<Long, ItemBaseline>();
      for (Item e: topkTextual) {
        ItemBaseline newItem = new ItemBaseline(e.getItemId(), alpha);
        newItem.setTextualScore(e.getTextualScore());
        items.put(e.getItemId(), newItem);
      }
      Item savedItem = null;
      for (Item e: topkSocial) {
        ItemBaseline newItem;
        if (items.containsKey(e.getItemId()))
          newItem = items.get(e.getItemId());
        else {
          newItem = new ItemBaseline(e.getItemId(), alpha);
          items.put(e.getItemId(), newItem);
        }
        if (e.getItemId() == 36599l)
          savedItem = e;
        newItem.setSocialScore(e.getSocialScore());
        newItem.setTextualScore(Math.max(e.getTextualScore(), newItem.getTextualScore()));
      }
      Set<ItemBaseline> ordered = new TreeSet<ItemBaseline>();
      for (long itemId: items.keySet()) {
        int s = ordered.size();
        if (savedItem.equals(items.get(itemId))) {
          System.out.println("what: ");
          System.out.println(savedItem);
          System.out.println(items.get(itemId));
        }
        ordered.add(items.get(itemId));
        if (ordered.size() <= s) {
          System.out.println("error " + itemId);
          System.out.println(items.get(itemId).getItemId());
          System.out.println(items.get(itemId).getScore());
          System.out.println(ordered.contains(items.get(itemId)));
        }
      }
      List<Long> listBaseline = new ArrayList<Long>();
      i = 0;
      for (ItemBaseline e: ordered) {
        i++;
        System.out.println(e.getItemId() + ": " + e.getSocialScore() + ", "
                + e.getTextualScore() + ", "+ e.getScore());
        if (i <= k)
          listBaseline.add(e.getItemId());
      }
      return (float)NDCG.getNDCG(listBaseline, this.oracleNDCG, k);
    }
    return 0;
  }

}