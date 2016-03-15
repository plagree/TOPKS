package org.dbweb.socialsearch.topktrust.datastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dbweb.socialsearch.topktrust.algorithm.score.Score;

/**
 *
 * @author Silviu Maniu & Paul Lagrée
 */
public class Item implements Comparable<Item> {

  private final long itemId;
  private Score score;
  private String bestCompletion;
  private Map<String,ItemWordData> mapWordsData = new HashMap<String, ItemWordData>();
  private float alpha = 0;
  private float worstscore = 0;
  private float worstscore_without_prefix = 0;
  private float bestscore = Float.POSITIVE_INFINITY;

  public Item(long itemId, float alpha, Score score) {
    this.itemId = itemId;
    this.alpha = alpha;
    this.score = score;
  }

  public void addTag(String tag, boolean isCompletion, float idf, int pos) {
    ItemWordData itemWordData = new ItemWordData(isCompletion, idf, pos);
    this.mapWordsData.put(tag, itemWordData);
  }

  /**
   * Filter the Item when a new word has been started in the incremental
   * version of the algorithm
   * @param previousWord Final keyword of the last prefix
   * @return true if the Item is still relevant, false otherwise
   */
  public boolean filterNextWord(String previousWord) {
    if (!this.mapWordsData.containsKey(previousWord))
      return false;
    for (String tag: this.mapWordsData.keySet()) {
      if (tag.equals(previousWord))
        this.mapWordsData.get(tag).setCompletion(false);
      else if (this.mapWordsData.get(tag).isCompletion())
        this.mapWordsData.remove(tag);
    }
    this.computeWorstScore();
    return true;
  }

  /**
   * Filter the Item when a new letter has been added to the final prefix in
   * the incremental version of the algorithm
   * @param newPrefix Prefix incremented of one letter
   * @return true if the Item is still relevant, false otherwise
   */
  public boolean filterNextLetter(String newPrefix) {
    boolean goodItem = false;
    for (String tag: this.mapWordsData.keySet()) {
      if (!this.mapWordsData.get(tag).isCompletion())
        goodItem = true;
      else if (!tag.startsWith(newPrefix))
        this.mapWordsData.remove(tag);
    }
    this.computeWorstScore();
    return goodItem;
  }

  /**
   *  Get ItemWordData for a given tag
   * @param tag
   * @return
   */
  public ItemWordData getItemWordData(String tag) {
    if (this.mapWordsData.containsKey(tag))
      return this.mapWordsData.get(tag);
    else
      return null;
  }

  /**
   *  Check if a keyword was added to the item
   * @param keyword
   * @return
   */
  public boolean containsTag(String keyword) {
    return this.mapWordsData.containsKey(keyword);
  }

  /**
   * Update social score of a given existing tag
   * @param tag
   * @param value
   */
  public void updateSocialScore(String tag, float value) {
    float prevUFVal = this.mapWordsData.get(tag).getUF();
    int prevR = this.mapWordsData.get(tag).getNbUsersSeen();
    this.mapWordsData.get(tag).setUF(prevUFVal + value);
    this.mapWordsData.get(tag).setNbUsersSeen(prevR + 1);
    this.updateWorstScore(tag);
  }

  /**
   * We got a new value for the TF (term document frequency)
   * @param tag
   * @param tdf
   */
  public void updateTDFScore(String tag, int tdf) {
    this.mapWordsData.get(tag).setTdf(tdf);
    this.updateWorstScore(tag);
  }

  /**
   * Compute the best score possible for the item, given the state of all its
   * parameters (e.g. TF value, social score gathered so far, ...). Keep in
   * mind that the value of the upper bound (best social score) changes
   * every time a new user is visited (if its similarity to the seeker changed
   * compared to previous visited user)
   * @param query
   * @param topReadingHead
   * @param userWeights
   * @return Best score (upper bound)
   */
  public float computeBestScore(Map<Integer, ReadingHead> topReadingHead,
      Map<Integer, Float> userWeights) {

    this.bestscore = 0;
    float best_score_completion = 0, current_score_completion = 0, uw = 0;
    float social = 0, tf = 0, comb = 0; // Social, textual and combined values
    int position;
    for(String tag : this.mapWordsData.keySet()) {
      uw = 0;
      position = this.mapWordsData.get(tag).getPosition();
      if (userWeights.containsKey(position))
        uw = userWeights.get(position);
      tf = 0;
      if (this.mapWordsData.get(tag).receivedTDFValue()) {
        tf = this.mapWordsData.get(tag).getTdf();
      }
      else if(topReadingHead.containsKey(position) &&
          (topReadingHead.get(position) != null)) {
        tf = topReadingHead.get(position).getValue();  // top value IL
      }
      // Social score (so far)
      social = this.mapWordsData.get(tag).getUF();
      // Number of users found who tagged this item (so far)
      float nbUsersSeen = this.mapWordsData.get(tag).getNbUsersSeen();
      social += (tf - nbUsersSeen) * uw;
      comb = this.alpha * tf + (1 - this.alpha) * social;
      if (this.mapWordsData.get(tag).isCompletion()) {
        current_score_completion = this.score.getScore(comb,
            this.mapWordsData.get(tag).getIdf());
        if (current_score_completion > best_score_completion) {
          best_score_completion = current_score_completion;
          this.bestCompletion = tag;
        }
      } else {
        this.bestscore += this.score.getScore(comb,
            this.mapWordsData.get(tag).getIdf());
      }
    }
    this.bestscore += best_score_completion;
    return this.bestscore;
  }

  /**
   * This method should not be needed if we only do small updates
   */
  public void computeWorstScore() {
    float wscore_without_completion = 0, wscore_best_completion = 0;
    for (String tag : this.mapWordsData.keySet()) {
      float wpartial = this.mapWordsData.get(tag)
          .computeWorstScore(this.alpha, this.score);
      if (this.mapWordsData.get(tag).isCompletion()
          && wpartial > wscore_best_completion)
        wscore_best_completion = wpartial;
      else
        wscore_without_completion += wpartial;
    }
    this.worstscore = wscore_without_completion + wscore_best_completion;
    this.worstscore_without_prefix = wscore_without_completion;
  }

  /**
   * Update the score of the Item given we changed only one tag
   * @param tag Modified tag value
   */
  private void updateWorstScore(String tag) {
    float old_wscore = this.mapWordsData.get(tag).getWorstScore();
    float new_wscore = this.mapWordsData.get(tag)
        .computeWorstScore(this.alpha, this.score);
    // We update only if the total worst score increased
    if (this.mapWordsData.get(tag).isCompletion()) {
      if (this.worstscore < this.worstscore_without_prefix + new_wscore)
        this.worstscore = this.worstscore_without_prefix + new_wscore;
    } else {
      this.worstscore += (new_wscore - old_wscore);
    }
  }

  /**
   * Get the social score of this item (total score max among all completions) SLOW
   * @return
   */
  public float getSocialScore() {
    float socialScore = 0, bestPrefixScore = 0, currentPrefixScore;
    String bestPrefix = null;
    for (String word: this.mapWordsData.keySet()) {
      if (this.mapWordsData.get(word).isCompletion()) { // the word is a prefix
        currentPrefixScore = this.mapWordsData.get(word)
            .computeWorstScore(this.alpha, this.score);
        if (currentPrefixScore >= bestPrefixScore) {
          bestPrefixScore = currentPrefixScore;
          bestPrefix = word;
        }
      } else { // the word is not a prefix
        socialScore += this.mapWordsData.get(word).computeSocialScore(this.score);
      }
    }
    if (bestPrefix == null) {
      System.out.println("Erreur de null pointeur l435 Item");
      System.exit(1);
    }
    socialScore += this.mapWordsData.get(bestPrefix).computeSocialScore(this.score);
    return socialScore;
  }

  /**
   * Get the textual score of this item (total score max among all completions)
   * @return
   */
  public float getTextualScore() {
    float textualScore = 0, bestPrefixScore = 0, currentPrefixScore;
    String bestPrefix = null;
    for (String word: this.mapWordsData.keySet()) {
      if (this.mapWordsData.get(word).isCompletion()) { // the word is a prefix
        currentPrefixScore = this.mapWordsData.get(word)
            .computeWorstScore(this.alpha, this.score);
        if (currentPrefixScore >= bestPrefixScore) {
          bestPrefixScore = currentPrefixScore;
          bestPrefix = word;
        }
      } else { // the word is not a prefix
        textualScore += this.mapWordsData.get(word).computeTextualScore(this.score);
      }
    }
    if (bestPrefix == null) {
      System.out.println("Erreur de null pointeur l457 Item");
      System.exit(1);
    }
    textualScore += this.mapWordsData.get(bestPrefix).computeTextualScore(this.score);
    return textualScore;
  }

  /**
   * Returns the List of textual heuristics for ChooseBranch routine.
   * @param nbWord Number of words in the query
   * @param topIL Value of the IL reading head
   * @return List of the textual heuristic for each word
   */
  public List<Float> getTextualBranchHeuristic(int nbWord,
      Map<Integer,ReadingHead> topReadingHead) {
    List<Float> res = new ArrayList<Float>();
    int pos = 0;
    for (pos = 0; pos < nbWord; pos++)
      res.add(0f);
    float curContrib = 0;
    for (String tag: this.mapWordsData.keySet()) {
      if (!this.mapWordsData.get(tag).isCompletion()
          || tag.equals(this.bestCompletion)) {
        pos = this.mapWordsData.get(tag).getPosition();
        curContrib = this.mapWordsData.get(tag).getTextualBranchHeuristic(
            this.alpha, topReadingHead.get(pos).getValue());
        res.set(pos, curContrib);
      }
    }
    return res;
  }

  /**
   * Returns the List of social heuristics for ChooseBranch routine.
   * @param nbWord Number of words in the query
   * @param topReadingHead
   * @param userWeights
   * @return List of the social heuristic for each word
   */
  public List<Float> getSocialBranchHeuristic(int nbWord,
      Map<Integer,ReadingHead> topReadingHead,
      Map<Integer, Float> userWeights) {
    List<Float> res = new ArrayList<Float>();
    int pos = 0;
    for (pos = 0; pos < nbWord; pos++)
      res.add(0f);
    float curContrib = 0;
    for (String tag: this.mapWordsData.keySet()) {
      if (!this.mapWordsData.get(tag).isCompletion()
          || tag.equals(this.bestCompletion)) {
        pos = this.mapWordsData.get(tag).getPosition();
        curContrib = this.mapWordsData.get(tag).getSocialBranchHeuristic(
            this.alpha, topReadingHead.get(pos).getValue(),
            userWeights.get(pos));
        res.set(pos, curContrib);
      }
    }
    return res;
  }

  public int compareTo(Item o) {
    if(o.getComputedWorstScore() > this.getComputedWorstScore())
      return 1;
    else if (o.getComputedWorstScore() < this.getComputedWorstScore())
      return -1;
    else {
      if(o.getBestscore() > this.getBestscore())
        return 1;
      else if(o.getBestscore() < this.getBestscore())
        return -1;
      else
        return Long.compare(o.getItemId(), this.itemId);
    }
  }

  @Override
  public boolean equals(Object o){
    if(!(o instanceof Item))
      return false;
    if (o == this)
      return true;
    Item rhs = (Item)o;
    return this.itemId == rhs.itemId;
  }

  @Override
  public int hashCode() {
    int hash = 1;
    hash = hash * 23 + (int)(this.itemId ^ (this.itemId >>> 32));
    return hash;
  }

  @Override
  public String toString(){
    return String.valueOf(itemId);
  }

  // Getters and Setters
  public float getComputedWorstScore() {
    return this.worstscore;
  }

  public double getBestscore() {
    return this.bestscore;
  }

  public long getItemId(){
    return this.itemId;
  }

  /**
   * Returns the completion leading to the best score.
   * @return Completion
   */
  public String getCompletion() {
    String completion = "";
    float bestScore = -1;
    for (String tag: this.mapWordsData.keySet()) {
      if (!this.mapWordsData.get(tag).isCompletion())
        continue;
      if (this.mapWordsData.get(tag).getWorstScore() > bestScore) {
        completion = tag;
      }
    }
    return completion;
  }

  public void debugging() {
    //System.out.println("Number of users seen: "+this.nbUsersSeen.toString());	  // number of users seen
    //System.out.println("Tag in: "+this.tags.toString()); // 1 for tag in
    //System.out.println("Sum of weights of users found: "+this.uf.toString());  // sum of weights of users found
    //System.out.println("IDF of tag: "+this.idf.toString()); // IDF of tag
    //System.out.println("Real TDF in IL: "+this.tdf.toString());  // real tdf in ILs
    //System.out.println("alpha: "+this.alpha);
    //System.out.println("score: "+this.score.toString());TODO
  }
}
