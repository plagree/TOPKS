package org.dbweb.socialsearch.topktrust.datastructure;

import org.dbweb.socialsearch.topktrust.algorithm.Pair;

/**
 * Reading head on an inverted list
 * 
 * @author Paul Lagrée
 */
public class ReadingHead implements Comparable<ReadingHead> {

  private int value;
  private Pair<Long, String> itemKeywordPair;

  /**
   * 
   * @param completion Inverted list term
   * @param item Item of the reading head
   * @param value Value of the reading head
   */
  public ReadingHead(String completion, long item, int value) {
    this.itemKeywordPair = new Pair<Long, String>(item, completion);
    this.value = value;
  }

  public String getCompletion() {
    return this.itemKeywordPair.getRight();
  }

  public long getItemId() {
    return this.itemKeywordPair.getLeft();
  }

  public Pair<Long, String> getItemKeywordPair() {
    return this.itemKeywordPair;
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }

  @Override
  public int compareTo(ReadingHead obj) {
    return -(this.value - obj.value);
  }

  @Override
  public int hashCode() {
    return this.itemKeywordPair.hashCode();
  }

  @Override
  public String toString() {
    return "Reading head of term " + this.itemKeywordPair.getRight() + " with value " + 
            this.value + " and item " + this.itemKeywordPair.getLeft(); 
  }
}
