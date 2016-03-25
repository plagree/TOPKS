package org.externals.Tools;

/**
 * Useful only for baseline experiments.
 * @author paul
 *
 */
public class ItemBaseline implements Comparable<ItemBaseline> {

  private float ts, sc, alpha;
  private long itemId;

  public ItemBaseline(long itemId, float alpha) {
    this.ts = 0;
    this.sc = 0;
    this.itemId = itemId;
    this.alpha = alpha;
  }
  
  public void setSocialScore(float sc) {
    this.sc = sc;
  }
  
  public void setTextualScore(float ts) {
    this.ts = ts;
  }
  
  public float getSocialScore() {
    return this.sc;
  }
  
  public float getTextualScore() {
    return this.ts;
  }
  
  public float getScore() {
    return this.alpha * this.ts + (1 - this.alpha) * this.sc;
  }
  
  public long getItemId() {
    return this.itemId;
  }
  
  public int compareTo(ItemBaseline o) {
    if(o.getScore() > this.getScore())
      return 1;
    else if (o.getScore() < this.getScore())
      return -1;
    return 0;
  }

  @Override
  public boolean equals(Object o){
    if(!(o instanceof ItemBaseline))
      return false;
    if (o == this)
      return true;
    ItemBaseline rhs = (ItemBaseline)o;
    return this.itemId == rhs.itemId;
  }
  
  @Override
  public int hashCode() {
    int hash = 1;
    hash = hash * 31 + (int)(this.itemId ^ (this.itemId >>> 32));
    return hash;
  }
  
  @Override
  public String toString() {
    return "(" + this.itemId + ", " + this.getScore() + ")";
  }
}