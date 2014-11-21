TOPKS
=====

Getting started
---------------

The libs/ folder contains all the different dependencies. Add them to the build path.

Before running the TOPKS program, a few modifications must be done in the code.

In org.dbweb.socialsearch.shared.Params, change the network to the name of the network
table used.

In org.dbweb.Arcomem.Integration.Test, change:
* dbConn: database ids
* query1: query to execute
* seekers: seeker who executes the query1
* network: network table name
* taggers: tagging table name (user, item, tag)
* k: number of results

Run the code, the answer of the query is in an XML file whose name looks like
tests_exact_soc_snet_tt_path_mult.xml.

If the dataset is very small, change the skipped_tests variable in the TopKAlgorithm
class (line 1068) to a small number, like 1.


Code explainations
------------------

Here are some importants structures used in the TOPKS-ASYT algorithm:
* `positions`: HashMap<String, Integer>, gives the position in the Inverted List of
the current document for a given keyword.
* `dictionaryTrie`: PatriciaTrie<String>, trie built with all the keywords. Used to have
access to all possible completions for a given prefix.
* `completion_trie`: RadixTreeImpl, trie with inverted lists at the leaves (actually,
in the current implementation they are in `docs2`) and with
a value at each node corresponding to the max of values of its children. A node also
has a reference to the best descendant to make updates easier when looking at a the
following document of an inverted list.
* `high_docs_query`: HashMap<String, Integer>, returns the tf or the current document
of a given keyword in the query.
* `next_docs2`: HashMap<String, String>, returns the current document of an Inverted
List.
* `docs2`: HashMap<String, ArrayList<DocumentNumTag>>, inverted list for each keyword
in the dictionary.
* `docs_users`: HashMap<Integer, PatriciaTrie<HashSet<String>>>, user space of every
user. For a given userId, the PatriciaTrie contains all the keywords used by the user
with the corresponding items
