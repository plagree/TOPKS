package org.dbweb.experiments;

import java.util.List;

import org.dbweb.completion.trie.RadixTreeNode;
import org.dbweb.socialsearch.topktrust.algorithm.TopKAlgorithm;

public class Baseline {

  /**
   * Method to execute a query using a baseline algorithm to compare TOPKS-ASYT with.
   * @param seeker Id of the user issuing the query
   * @param query List of words in the query (last keyword corresponds to a prefix)
   * @param k Number of results in the final result (top-k response)
   * @param t Maximum time to give an answer
   * @param newQuery Boolean value which is true if this is a new query (for instantiations)
   * @param nVisited Maximum number of users we are allowed to visit
   * @return TODO Try to put the whole code in another place
   */
  /*public int[] executeSocialBaselineQuery(TopKAlgorithm alg, int seeker,
      List<String> query, int k, int t, boolean newQuery, int nVisited) {
    String prefix = query.get(0);
    RadixTreeNode radixTreeNode = completionTrie.searchPrefix(prefix, false);
    RadixTreeNode originalNode = radixTreeNode.clone();
    radixTreeNode.setBestDescendant(radixTreeNode);
    radixTreeNode.setReal(true);
    radixTreeNode.setWord(prefix);

    // Union of inverted lists of possible completions
    long timeBefore = System.nanoTime();
    Map<String, Integer> indexPosition = new HashMap<String, Integer>();
    Queue<ReadingHead> queue = new PriorityQueue<ReadingHead>();

    Map<String, String> completions = this.dictionaryTrie.prefixMap(prefix);
    Iterator<Entry<String, String>> iterator = completions.entrySet().iterator();
    Entry<String, String> currentEntry = null;
    DocumentNumTag firstDoc = null;
    //int nbLoadedBlocksFromDisk = 0;
    int nbInvertedListsForMerge = 0;
    while (iterator.hasNext()) {
      currentEntry = iterator.next();
      String completion = currentEntry.getKey();
      indexPosition.put(completion, 0);
      firstDoc = this.invertedLists.get(completion).get(0);
      //nbLoadedBlocksFromDisk += this.sizeInvertedLists.get(completion) / Params.SIZE_OF_BLOCK + 1;
      nbInvertedListsForMerge += 1;
      queue.add(new ReadingHead(completion, firstDoc.getDocId(), firstDoc.getNum()));
    }

    List<DocumentNumTag> mergedList = new ArrayList<DocumentNumTag>(); // Output of the merge of inverted lists
    ReadingHead currentHead = null;
    String completion = null;
    this.correspondingCompletions = new ArrayList<String>();
    while (!queue.isEmpty()) {
      currentHead = queue.poll();
      mergedList.add(new DocumentNumTag(currentHead.getItem(), currentHead.getValue()));
      completion = currentHead.getCompletion();
      this.correspondingCompletions.add(completion); // NO MAX
      int count = indexPosition.get(completion);
      indexPosition.put(completion, count+1);
      if (count + 1 < this.invertedLists.get(completion).size()) {
        firstDoc = this.invertedLists.get(completion).get(count + 1);
        currentHead.setValue(firstDoc.getNum());
        currentHead.setItem(firstDoc.getDocId());
        queue.add(currentHead);
      }
    }
    System.out.println(mergedList.size()+" size of merged list");
    Params.NUMBER_ILS = nbInvertedListsForMerge;

    List<DocumentNumTag> originalList = null;
    if (this.invertedLists.containsKey(prefix))
      originalList = this.invertedLists.get(prefix);

    this.invertedLists.put(prefix, mergedList);
    boolean prefix_not_a_word = false;
    if (!this.invertedListPositions.containsKey(prefix)) {
      this.invertedListPositions.put(prefix, 0);
      prefix_not_a_word = true;
    }
    long timeToMerge = (System.nanoTime() - timeBefore) / 1000000;

    // Execute query with materialised list
    long timeBeforeQuery = System.nanoTime();
    this.executeQuery(seeker, query, k, t, nVisited);
    Params.NUMBER_ILS = 0;

    radixTreeNode.setBestDescendant(originalNode.getBestDescendant());
    radixTreeNode.setReal(originalNode.isReal());
    radixTreeNode.setWord(originalNode.getWord());
    radixTreeNode.setChildren(originalNode.getChildren());
    radixTreeNode.updatePreviousBestValue(originalNode.getValue());
    if (originalList != null)
      this.invertedLists.put(prefix, originalList);
    if (prefix_not_a_word)
      this.invertedListPositions.remove(prefix);
    else
      this.invertedListPositions.put(prefix, 0);
    long timeQuery = (System.nanoTime() - timeBeforeQuery) / 1000000;
    int res[] = {(int)timeToMerge, (int)timeQuery, nbInvertedListsForMerge, this.invertedListsUsed.size()};
    this.correspondingCompletions = null;

    return res;
  }*/

}
