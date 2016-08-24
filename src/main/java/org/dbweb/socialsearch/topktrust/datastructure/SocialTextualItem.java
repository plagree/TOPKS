package org.dbweb.socialsearch.topktrust.datastructure;

public class SocialTextualItem implements Comparable {

  private int docId;
  private float textual_score;
  private float social_score;
  private float alpha;
  
  public SocialTextualItem(int docId, float t_score, float s_score, float alpha) {
    this.docId = docId;
    this.textual_score = t_score;
    this.social_score = s_score;
    this.alpha = alpha;
  }
  
  public int getDocId() {
    return this.docId;
  }
  
  public float getSocialScore() {
    return this.social_score;
  }
  
  public void setSocialScore(float sc) {
    this.social_score = sc;
  }
  
  public float getScore() {
    return (1 - this.alpha) * this.social_score + this.alpha * this.textual_score;
  }
  
  @Override
  public int compareTo(Object obj) {
    if(!(obj instanceof SocialTextualItem))
      return 1;
    SocialTextualItem o = (SocialTextualItem)obj;
    if(o.getScore() > this.getScore())
      return 1;
    else if (o.getScore() < this.getScore())
      return -1;
    else 
      return 0;
  }

  @Override
  public boolean equals(Object o){
    if(!(o instanceof SocialTextualItem))
      return false;
    if (o == this)
      return true;
    SocialTextualItem rhs = (SocialTextualItem)o;
    return this.getScore() == rhs.getScore();
  }

  @Override
  public int hashCode() {
    int hash = 1;
    hash = hash * 29 + (int)(this.docId ^ (this.docId >>> 32));
    return hash;
  }

  @Override
  public String toString() {
    String res = "Item=" + String.valueOf(this.docId) + ", score=" + this.getScore();
    return res;
  }
}
