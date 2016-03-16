package org.dbweb.topktrust.socialsearch.importer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.dbweb.completion.trie.RadixTreeImpl;
import org.dbweb.socialsearch.shared.Params;
import org.dbweb.socialsearch.topktrust.algorithm.DocumentNumTag;

import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

public class CSVFileImporter {

  private static long getUsedMemory() {
    return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
  }

  /**
   * Method to load file with triples in memory
   * @throws IOException
   */
  public static void loadInMemory(RadixTreeImpl completionTrie,
          RadixTreeImpl tagIdf, Map<String, Integer> invertedListPositions,
          Map<String, List<DocumentNumTag>> invertedLists,
          PatriciaTrie<String> dictionaryTrie,
          TIntObjectMap<PatriciaTrie<TLongSet>> userSpaces)
                  throws IOException {

    final long start = getUsedMemory();
    // Positions for a given keyword in the graph (useful for multiple words)
    Map<String, Set<Long>> tagPopularity = new HashMap<String, Set<Long>>();
    TIntIntHashMap sizeUserSpaces = new TIntIntHashMap();
    Map<String, Integer> sizeInvertedLists = new HashMap<String, Integer>();

    BufferedReader br;
    String line, tag;
    String[] data;
    int userId;
    long itemId;

    // 0. Initialisations of main variables specific to dataset
    br = new BufferedReader(new FileReader(Params.dir + Params.triplesFile));
    Set<Long> docs = new HashSet<Long>(); // Count distinct documents
    while ((line = br.readLine()) != null) {
      data = line.split("\t");
      if (data.length != 3)
        continue;
      if (data.length != 3)
        continue;
      userId = Integer.parseInt(data[0]);
      itemId = Long.parseLong(data[1]);
      tag = data[2];
      if (!tagPopularity.containsKey(tag))
        tagPopularity.put(tag, new HashSet<Long>());
      tagPopularity.get(tag).add(itemId);
      docs.add(itemId);
    }
    br.close();
    Params.number_documents = docs.size();

    // 1. Tag Inverted lists processing
    br = new BufferedReader(new FileReader(Params.dir + Params.ILFile));
    List<DocumentNumTag> currIL;
    int counter = 0;
    while ((line = br.readLine()) != null) {
      data = line.split("\t");
      if (data.length < 2)
        continue;
      tag = data[0];
      if (!invertedLists.containsKey(data[0]))
        invertedLists.put(tag, new ArrayList<DocumentNumTag>());
      currIL = invertedLists.get(data[0]);
      for (int i=1; i<data.length; i++) {
        String[] tuple = data[i].split(":");
        if (tuple.length != 2)
          continue;
        currIL.add(new DocumentNumTag(Long.parseLong(tuple[0]),
                Integer.parseInt(tuple[1])));
      }
      Collections.sort(currIL, Collections.reverseOrder());
      DocumentNumTag firstDoc = currIL.get(0);
      completionTrie.insert(tag, firstDoc.getNum());
      invertedListPositions.put(tag, 0);
      counter++;
      if ((counter % 50000) == 0 && Params.VERBOSE)
        System.out.println("\t" + counter + " tag ILs loaded");
    }
    br.close();
    final long size = (getUsedMemory() - start) / 1024 / 1024;
    if (Params.VERBOSE) {
      System.out.println("Inverted List file = " + size + "M");
      System.out.println("Inverted List file loaded.");
    }

    // 2. Triples processing
    final long start2 = getUsedMemory();
    br = new BufferedReader(new FileReader(Params.dir + Params.triplesFile));
    counter = 0;

    while ((line = br.readLine()) != null) {
      data = line.split("\t");

      if (data.length != 3)
        continue;
      userId = Integer.parseInt(data[0]);
      itemId = Long.parseLong(data[1]);
      tag = data[2];
      if (!dictionaryTrie.containsKey(tag))
        dictionaryTrie.put(tag, ""); // This trie has no value
      if(!userSpaces.containsKey(userId)){
        userSpaces.put(userId, new PatriciaTrie<TLongSet>());
      }
      if(!userSpaces.get(userId).containsKey(tag))
        userSpaces.get(userId).put(tag, new TLongHashSet());
      userSpaces.get(userId).get(tag).add(itemId);
      counter++;
      if ((counter % 1000000) == 0 && Params.VERBOSE)
        System.out.println("\t" + counter + " triples loaded");
    }
    br.close();
    final long size2 = (getUsedMemory() - start2) / 1024 / 1024;

    // 3. Computation of the space of user spaces and inverted lists
    TIntObjectIterator<PatriciaTrie<TLongSet>> it = userSpaces.iterator();
    while (it.hasNext()) { // Loop on userSpaces
      it.advance();
      int user = it.key();
      PatriciaTrie<TLongSet> patricia = userSpaces.get(user);
      Set<String> keys = patricia.keySet();
      int current_size = 0;
      for (String key: keys)
        current_size += key.length() + 1;
      Iterator<TLongSet> it2 = patricia.values().iterator();
      while (it2.hasNext()) {
        current_size += it2.next().size() * 8;
      }
      sizeUserSpaces.put(user, current_size);
    }
    Set<String> keys = invertedLists.keySet();
    for (String key: keys) {
      sizeInvertedLists.put(key, invertedLists.get(key).size()*8);
    }

    if (Params.VERBOSE)
      System.out.println("User spaces file = " + size2 + "M");
    Params.number_users = userSpaces.size();

    // 3. Tag Freq processing
    final long start3 = getUsedMemory();
    int tagpop;
    for (String tag2: tagPopularity.keySet()) {
      tagpop = tagPopularity.get(tag2).size();
      //float tagidf = (float) Math.log(((float)Params.number_documents
      //        - (float)tagfreq + 0.5)/((float)tagfreq+0.5));
      float tagidf = (float) Math.log(0.5 + (float)Params.number_documents
              / ((float) tagpop) ); // Old tf-idf
      tagIdf.insert(tag2, tagidf);
    }
    final long size3 = (getUsedMemory() - start3) / 1024 / 1024;

    if (Params.VERBOSE)
      System.out.println("TagFreq file = " + size3 + "M");
  }

}