package org.dbweb.socialsearch.topktrust.datastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.dbweb.completion.trie.RadixTreeImpl;
import org.dbweb.socialsearch.shared.Params;
import org.dbweb.socialsearch.topktrust.algorithm.Pair;
import org.dbweb.socialsearch.topktrust.algorithm.score.Score;

/**
 * Class corresponding to the candidate buffer. It also contains method to check
 * if the termination condition is met
 * @author Paul
 */
public class ItemList {

  private Map<Long,Item> items; // this is a map (for fast access to item pointers)
  private Score score;
  private double min_topk;
  private double max_rest;
  private int number_of_candidates;
  // Textual and social upper bounds of the most promising sub-optimal item
  private List<Float> textualBranchHeuristic;
  private List<Float> socialBranchHeuristic;
  private float max_unseen;
  private TreeSet<Item> sorted_items;
  private long bestSuboptimal;

  public ItemList(Score score) {
    this.min_topk = 0;
    this.max_rest = 0;
    this.number_of_candidates = 0;
    this.items = new HashMap<Long,Item>();
    this.sorted_items = new TreeSet<Item>();
    this.score = score;
  }

  /**
   * Updates social score of itemId for the specified tag. They both must exist.
   * @param itemId
   * @param tag
   * @param userWeight
   */
  public void updateSocialScore(long itemId, String tag, float userWeight) {
    Item item = this.items.get(itemId);
    this.sorted_items.remove(item);
    item.updateSocialScore(tag, userWeight);
    // After update, it must be re-added to the TreeSet
    this.sorted_items.add(item);
  }

  /**
   * Adds a new to an existing item.
   * @param itemId
   * @param completion
   * @param isCompletion
   * @param idf
   * @param pos
   */
  public void addTagToItem(long itemId, String completion, boolean isCompletion,
          float idf, int pos) {
    Item item = this.items.get(itemId);
    this.sorted_items.remove(item);
    item.addTag(completion, isCompletion, idf, pos);
    // After update, it must be re-added to the TreeSet
    this.sorted_items.add(item);
  }

  /**
   * Updates textual (and sometimes social) score of itemId for the specified
   * tag. They both must exist.
   * @param itemId
   * @param tag
   * @param completion
   */
  public void updateTextualScore(long itemId, String tag, int tdf) {
    Item item = this.items.get(itemId);
    this.sorted_items.remove(item);
    item.updateTDFScore(tag, tdf);
    // After update, it must be re-added to the TreeSet
    this.sorted_items.add(item);
  }

  /**
   * Returns ranking of item given in parameter, zero if not present in the
   * candidate list
   * @param itemId Id of the item we are looking for
   * @param k
   * @return Ranking of the item in the list
   */
  public int getRankingItem(long itemId, int k) {
    int pos = 1;
    for (Item currItem: this.sorted_items) {
      if (itemId == currItem.getItemId())
        return pos;
      pos++;
    }
    return 0;
  }

  /**
   * Adds a new Item in the candidates.
   * @param itemId
   * @param alpha
   */
  public void addItem(Long itemId, float alpha) {
    Item item = new Item(itemId, alpha, this.score);
    this.items.put(itemId, item);
    this.sorted_items.add(item);
  }

  /**
   * Checks if the specified itemId is in the candidate list
   * @param itemId Id of the item to check
   * @return true if it is in the ItemList, false otherwise
   */
  public boolean containsItemId(Long itemId) {
    return this.items.containsKey(itemId);
  }

  /**
   * Initialises the heuristics for ChooseBranch method
   * @param query
   * @param trie
   */
  public void setBranchHeuristics(List<String> query, RadixTreeImpl trie) {
    this.textualBranchHeuristic = new ArrayList<Float>();
    this.socialBranchHeuristic = new ArrayList<Float>();
    for(int pos = 0; pos < query.size(); pos++){
      if (pos == query.size() - 1) { // Prefix
        this.textualBranchHeuristic.add(
                trie.searchPrefix(query.get(pos), false).getValue());
        this.socialBranchHeuristic.add(
                trie.searchPrefix(query.get(pos), false).getValue());
      } else {
        this.textualBranchHeuristic.add(
                trie.searchPrefix(query.get(pos), true).getValue());
        this.socialBranchHeuristic.add(
                trie.searchPrefix(query.get(pos), true).getValue());
      }
    }
  }

  /**
   * Adds a new word in the ChooseBranch heuristic. (Note: It does not refine
   * the value for previous word (given we have the exact word now)
   * @param prefix Next prefix (prefix of new word)
   * @param trie
   */
  public void addWordBranchHeuristic(String prefix, RadixTreeImpl trie) {
    this.textualBranchHeuristic.add(
            trie.searchPrefix(prefix, false).getValue());
    this.socialBranchHeuristic.add(
            trie.searchPrefix(prefix, false).getValue());
  }

  /**
   * Removes the completions that don't match the new typed letter. (Note:
   * this method does not update ChooseBranch heuristics)
   * @param query
   */
  public void filterNextLetter(List<String> query) {
    Item item;
    this.sorted_items = new TreeSet<Item>();
    String newPrefix = query.get(query.size() - 1);
    for (long itemId: this.items.keySet()) {
      item = this.items.get(itemId);
      if (item.filterNextLetter(newPrefix) == true)  // Item is still relevant
        this.sorted_items.add(item);
      else
        this.items.remove(itemId);
    }
  }

  /**
   * Remove the completions which did not correspond to final keyword
   * @param query
   * @param tag_idf
   * @param trie
   */
  public void filterNextWord(List<String> query, RadixTreeImpl tag_idf,
          RadixTreeImpl trie, Set<Pair<Long, String>> unknownTf) {
    Item item;
    String previousWord = query.get(query.size() - 2);
    this.sorted_items = new TreeSet<Item>(); // Reset candidate sorted structure
    // We filter over all items in the hash map
    for (long itemId: this.items.keySet()) {
      item = this.items.get(itemId);
      if (item.filterNextWord(previousWord, unknownTf) == true) // Item is still relevant
        this.sorted_items.add(item);
      else {
        this.items.remove(itemId);
      }
    }
    this.addWordBranchHeuristic(query.get(query.size() - 1), trie);
  }

  /**
   * If time limit has been passed, we try to extract the best from what we found so far.
   * @param k
   * @param guaranteed
   * @param possible
   */
  /*public void extractProbableTopK(int k, Set<Long> guaranteed,
            Set<Long> possible, Map<Integer, ReadingHead> topReadingHead,
            Map<Integer,Float> userWeights, Map<String, Integer> positions) {

        int counter = 0;
        double wsc_t = 0;

        for (Item curr_item: sorted_items) {
            curr_item.computeBestScore(topReadingHead, userWeights);
        }
        List<Item> sorted_ws = new ArrayList<Item>(sorted_items);
        Collections.sort(sorted_ws);
        List<Item> sorted_bs = new ArrayList<Item>(sorted_items);
        Collections.sort(sorted_bs, new ItemBestScoreComparator());
        List<Item> possibleItems = new ArrayList<Item>();

        if(sorted_ws.size() >= k) 
            wsc_t = sorted_ws.get(k-1).getComputedWorstScore();
        else {
            for (Item item: sorted_ws)
                guaranteed.add(item.getItemId()+"#"+item.getCompletion());
            return;
        }
        for (Item item: sorted_ws) {
            counter = 0;
            for (Item item2: sorted_bs) {
                if (item.getComputedWorstScore() < item2.getBestscore())
                    counter += 1;
                else
                    break;
            }
            if ((counter<=k) && (this.score_unseen<=item.getComputedWorstScore()))
                guaranteed.add(item.getItemId()+"#"+item.getCompletion());
            else if(item.getBestscore() > wsc_t)
                possibleItems.add(item);
        }

        Collections.sort(possibleItems, new ItemAverageScoreComparator());
        int k_possible = k - guaranteed.size();
        Item current_item;
        for (int i = 0; i < k_possible; i++) {
            if (i >= possibleItems.size())
                break;
            current_item = possibleItems.get(i);
            possible.add(current_item.getItemId()+"#"+current_item.getCompletion());
        }
        return;
    }*/

  /**
   * Checks the top-k and tries to terminate the algorithm.
   * @param query
   * @param k
   * @param alpha
   * @param idf
   * @param topReadingHead
   * @param user_weights
   * @param needUnseen
   * @param guaranteed
   * @param possible
   * @return true if we can stop exploring the graph / ILs, false otherwise
   */
  public boolean terminationCondition(List<String> query, int k, float alpha,
          RadixTreeImpl idf, List<ReadingHead> topReadingHead,
          List<Float> userWeights, Set<Long> possible) {

    this.max_unseen = 0;  // Value of the upper bound estimation of unseen items
    float high_value = 0, uw = 0, textualpart = 0, socialpart = 0, total = 0;

    // Step 1: Upper bound on unseen items
    for (int pos = 0; pos < query.size(); pos++) {
      high_value = 0;
      if (topReadingHead.get(pos) != null)
        high_value = topReadingHead.get(pos).getValue();
      uw = userWeights.get(pos);
      textualpart = alpha * high_value;
      socialpart = (1 - alpha) * high_value * uw;
      total = textualpart + socialpart;
      if (pos == query.size() - 1) // Prefix
        this.max_unseen += this.score.getScore(total,
                idf.searchPrefix(query.get(pos), false).getValue());
      else
        this.max_unseen += this.score.getScore(total,
                idf.searchPrefix(query.get(pos), true).getValue());
      this.socialBranchHeuristic.set(pos, high_value);
      this.textualBranchHeuristic.set(pos, high_value);
    }

    // Step 2: Check the termination condition
    float scoremin = Float.POSITIVE_INFINITY, scoremax = 0, currentUB;
    int i = 0;
    long itemId;
    this.bestSuboptimal = -1;
    for (Item item: this.sorted_items) {
      i++;
      itemId = item.getItemId();
      if (i < k)
        continue;
      else if (i == k)
        scoremin = item.getComputedWorstScore();
      else if (possible.contains(itemId)) {
        currentUB = item.computeBestScore(query, topReadingHead, userWeights, idf);
        if (currentUB < scoremin) // This item can't be in the final list
          possible.remove(itemId);
        if (scoremax < currentUB) {
          scoremax = currentUB;
          this.bestSuboptimal = itemId;
        }
      }
    }

    // Step 3: Compute heuristics for ChooseBranch routine
    if (this.bestSuboptimal > 0 && alpha != 0f && alpha != 1f) {
      this.textualBranchHeuristic = this.items.get(this.bestSuboptimal)
              .getTextualBranchHeuristic(query.size(), topReadingHead);
      this.socialBranchHeuristic = this.items.get(this.bestSuboptimal)
              .getSocialBranchHeuristic(query.size(), topReadingHead, userWeights);
    }
    this.number_of_candidates = i;
    this.min_topk = scoremin;
    this.max_rest = Math.max(this.max_unseen, scoremax);

    if ((this.max_rest <= this.min_topk) && (this.number_of_candidates >= k))
      return true;
    else
      return false;
  }

  /**
   * Returns a List<Item> of the top-k of the current candidate buffer
   * @param k Number of items in the top-k
   * @return List of Item
   */
  public List<Item> getListTopk(int k) {
    List<Item> topk = new ArrayList<Item>();
    int i = 0;
    for (Item item: this.sorted_items) {
      i++;
      topk.add(item);
      if (i >= k)
        break;
    }
    return topk;
  }

  /**
   * Returns a List<Item> of the top-k of the current candidate buffer
   * @param k Number of items in the top-k
   * @return List of Item
   */
  public List<Long> getLongListTopk(int k) {
    List<Long> topk = new ArrayList<Long>();
    int i = 0;
    for (Item item: this.sorted_items) {
      topk.add(item.getItemId());
      i++;
      if (i == k)
        break;
    }
    return topk;
  }

  /**
   * Returns a Set<Long> of the top-k of the current candidate buffer
   * @param k Number of items in the top-k
   * @return Set of Long
   */
  public Set<Long> getSetTopk(int k) {
    Set<Long> topk = new HashSet<Long>();
    for (Item item: this.sorted_items) {
      topk.add(item.getItemId());
    }
    return topk;
  }

  public int getNumberOfSortedItems() {
    return this.sorted_items.size();
  }

  public double getMaxRest() {
    return max_rest;
  }

  /**
   * Textual heuristic for ChooseBranch routine, must be called after after
   * the terminationCondition method.
   * @param pos
   * @return
   */
  public float getTextualBranchHeuristic(int pos) {
    return this.textualBranchHeuristic.get(pos);
  }

  /**
   * Social heuristic for ChooseBranch routine, must be called after after
   * the terminationCondition method.
   * @param pos
   * @return
   */
  public float getSocialBranchHeuristic(int pos) {
    return this.socialBranchHeuristic.get(pos);
  }

  public Map<Long,Item> getItems() {
    return items;
  }

  public Item getItem(long itemId) {
    return this.items.get(itemId);
  }

  /**
   * Update alpha values of Item objects (useful for textual -> social baseline)
   * @param alpha New alpha value
   */
  public void updateAlpha(float alpha) {
    for (long itemId: this.items.keySet()) {
      Item item = this.items.get(itemId);
      this.sorted_items.remove(item);
      item.updateAlpha(alpha);
      // After update, it must be re-added to the TreeSet
      this.sorted_items.add(item);
    }
  }

  public void debug(int k) {
    int i = 0;
    for (Item e: this.sorted_items) {
      i++;
      System.out.println(e);
      if (i >= k)
        break;
    }
  }

}