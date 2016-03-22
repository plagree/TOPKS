#!/bin/bash
# Script for experimentation

PATH_JAR=~/Workspace/TOPKS/target/Topks-0.1.jar   # path to jar
N_DOCS=18149                    # Number of documents
QUERIES=queries.txt             # Name of file for queries
#RESULTS=supernodes
#CLUSTERS=clusters_5000_nodes.txt


# Simple Tests
java -jar -Xmx3000m $PATH_JAR ~/Workspace/TOPKS/test/yelp/TOPZIP/small/ $N_DOCS network.txt triples.txt 0
##pid=$!
##echo $pid
##sleep 100
##python script.py $QUERIES 5000nodes/$CLUSTERS
##kill -9 $pid
