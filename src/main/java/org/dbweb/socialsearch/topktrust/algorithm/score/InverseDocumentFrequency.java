package org.dbweb.socialsearch.topktrust.algorithm.score;

public class InverseDocumentFrequency {

  public static float unary(int tagPopularity, int numberDocuments) {
    return 1f;
  }

  public static float classic(int tagPopularity, int numberDocuments) {
    return (float)Math.log((double)numberDocuments / tagPopularity);
  }

  public static float smooth(int tagPopularity, int numberDocuments) {
    return (float)Math.log(1 + (double)numberDocuments / tagPopularity);
  }

  public static float halfSmooth(int tagPopularity, int numberDocuments) {
    return (float)Math.log(0.5 + (double)numberDocuments / tagPopularity);
  }
  
  public static float unknown(int tagpop, int D) {
    
    return (float)Math.log(((float)D - (float)tagpop + 0.5)
            / ((float)tagpop + 0.5));
  }

  public static float probabilistic(int tagPopularity, int numberDocuments) {
    return (float)Math.log((double)(numberDocuments - tagPopularity)
            / tagPopularity);
  }

}
