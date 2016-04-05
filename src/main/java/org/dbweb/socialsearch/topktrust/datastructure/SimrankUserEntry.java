package org.dbweb.socialsearch.topktrust.datastructure;

public class SimrankUserEntry implements Comparable<SimrankUserEntry> {

  private int userId;
  private float simrank;

  public SimrankUserEntry(int userId, float sim) {
    this.userId = userId;
    this.simrank = sim;
  }

  public int getUserId(){
    return this.userId;
  }

  public float getSimrank(){
    return this.simrank;
  }

  public void setSimrank(float sim) {
    this.simrank = sim;
  }

  @Override
  public int compareTo(SimrankUserEntry o) {
    return Float.compare(this.simrank, o.simrank);
  }

}