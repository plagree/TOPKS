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

