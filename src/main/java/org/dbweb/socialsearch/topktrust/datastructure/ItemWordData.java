package org.dbweb.socialsearch.topktrust.datastructure;

import org.dbweb.socialsearch.topktrust.algorithm.score.Score;

public class ItemWordData {

  private float idf; 			      // idf for each term of the query
  private float uf; 				    // sum of user similarities found (social score?)
  private int tdf; 				      // number of users who tagged this item for each term (found in IL)
  private int nbUsersSeen; 		  // number of users seen who tagged this item for each term
  private boolean isCompletion;	// Is it a completion?
  private float worstScore;
  private int position;			    // Position of the word in the query

  public ItemWordData(boolean isCompletion, float idf, int pos) {
    this.idf = idf;
    this.uf = 0;
    this.tdf = -1;				// We don't know
    this.nbUsersSeen = 0;
    this.isCompletion = isCompletion;
    this.worstScore = 0;
    this.position = pos;
  }

  public float computeWorstScore(float alpha, Score score) {
    float wsocial = 0;
    float wnormal = 0;
    if(this.tdf >= 0) {
      wnormal = this.tdf;
    } else {
      wnormal = this.nbUsersSeen;
    }
    wsocial = this.uf;
    this.worstScore = score.getScore(
        alpha * wnormal + (1 - alpha) * wsocial, this.idf);
    return this.worstScore;
  }

  public float computeSocialScore(Score score) {
    return (float)score.getScore(this.uf, this.idf);
  }

  public float computeTextualScore(Score score) {
    if (this.tdf >= 0) // the item has been seen in IL
      return (float)score.getScore(this.tdf, this.idf);
    // we haven't met this item in the IL of word yet
    return (float)score.getScore(this.nbUsersSeen, this.idf);
  }

  public void updateSocialScore(float userWeight) {

  }

  public float getIdf() {
    return idf;
  }

  public void setIdf(float idf) {
    this.idf = idf;
  }

  public float getUF() {
    return uf;
  }

  public void setUF(float uf) {
    this.uf = uf;
  }

  public int getTdf() {
    return tdf;
  }

  public void setTdf(int tdf) {
    this.tdf = tdf;
  }

  public int getNbUsersSeen() {
    return nbUsersSeen;
  }

  public void setNbUsersSeen(int nbUsersSeen) {
    this.nbUsersSeen = nbUsersSeen;
  }

  public boolean isCompletion() {
    return isCompletion;
  }

  public void setCompletion(boolean isCompletion) {
    this.isCompletion = isCompletion;
  }

  public boolean receivedTDFValue() {
    return this.tdf >= 0;
  }

  public void setWorstScore(float ws) {
    this.worstScore = ws;
  }

  public float getWorstScore() {
    return this.worstScore;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  /**
   * Gives the textual heuristic for this given word
   * @param alpha Parameter to balance social and textual score
   * @param topIL Value of the current IL reading head
   * @return Textual heuristic
   */
  public float getTextualBranchHeuristic(float alpha, int topIL) {
    if (this.tdf < 0)   // We haven't read the IL yet
      return alpha * topIL;
    else
      return 0;
  }

  /**
   * Gives the social heuristic for this given word
   * @param alpha Parameter to balance social and textual score
   * @param topIL Value of the current IL reading head
   * @param userWeight Value of the current visited user
   * @return Social heuristic
   */
  public float getSocialBranchHeuristic(float alpha, int topIL, float userWeight) {
    if (this.tdf < 0)   // We haven't read the IL yet
      return (1 - alpha) * topIL * userWeight;
    else
      return (1 - alpha) * (this.tdf - this.nbUsersSeen) * userWeight;
  }

}
