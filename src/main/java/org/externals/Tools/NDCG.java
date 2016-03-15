package org.externals.Tools;

import java.util.Arrays;
import java.util.List;

public class NDCG {

    public static void main(String[] args) {
        List<Long> urls = Arrays.asList(new Long[] { 42153l, 19726l, 48017l, 27880l, 53486l, 6237l, 121l, 49267l, 42602l, 35266l, 45080l, 35116l, 52705l, 27486l, 36573l, 6849l, 5735l, 27879l, 53194l, 52526l });
        List<Long> oracleUrls = Arrays.asList(new Long[] { 27880l, 42153l, 121l, 27488l, 52343l, 122l, 19726l, 52700l, 50165l, 52705l, 48017l, 19439l, 12824l, 11612l, 12640l, 53486l, 7571l, 52816l, 36720l, 52380l });
        System.out.println(NDCG.getNDCG(urls, oracleUrls, 20));
    }

    public static double getNDCG(List<Long> items, List<Long> oracleItems, int r) {
        // get DCG of items
        double itemDCG = getDCG(items, oracleItems, Math.min(r, items.size()));

        // get DCG of perfect ranking
        double perfectDCG = getDCG(oracleItems, oracleItems, Math.min(r, oracleItems.size()));

        // normalise by dividing
        double normalized = itemDCG / perfectDCG;
        return normalized;
    }

    private static double getDCG(List<Long> items, List<Long> oracleItems, int p) {
        double score = 0;

        for (int i = 0; i < p; i++) {

            double relevance = getRelevance(items.get(i), oracleItems);
            int ranking = i + 1;

            if (ranking > 1) {
                // for all positions after the first one, reduce the "gain" as ranking increases
                relevance /= logBase2(ranking);
            }

            score += relevance;
        }

        return score;
    }

    private static double getRelevance(Long url, List<Long> oracleItems) {
        // Use the position in the oracle ranking as the relevance
        if (oracleItems.contains(url))
            return oracleItems.size() - oracleItems.indexOf(url);
        return 0;
    }

    private static double logBase2(double value) {
        return Math.log(value) / Math.log(2);
    }

}