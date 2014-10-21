#!/bin/bash
# args: warc file, crawl_name, db_username(by default arcomem), hadoop_home 
# tmp_socsearch is an intermediate dir

if [ -n "$4" ]
  then export HADOOP_HOME=$4
fi

crawl_name=$2
db_username=$3

rm -rf tmp
mkdir tmp

echo "creating social search triples..."
#python lib/generate_socsearch_triples.py < $1 
cat $1 | sort -n | uniq >tmp/input.csv
python lib/convert_to_sql.py tagging<tmp/input.csv >tmp/triples.sql


###
# PostgreSQL
###
echo "creating socialdb..."
createdb $crawl_name -U $db_username
psql -U $db_username -d $crawl_name -f sqls/schema.sql
echo "populating socialdb..."
psql -U $db_username -d $crawl_name -f tmp/triples.sql
echo "  creating similarity networks"
#psql -U $db_username -d $crawl_name -f sqls/tagging_indexes.sql
echo Creating derived relations
psql -U $db_username -d $crawl_name -f sqls/create_docs_psql.sql
psql -U $db_username -d $crawl_name -f sqls/create_tagfreq_psql.sql

echo Computing similarities
hdfs dfs -rm -r tagging
hdfs dfs -put tmp/input.csv tagging
cd similarity
./hadoop_similarity_t2.sh
cd ..
echo Inserting similarities into DB
hdfs dfs -cat sim/t_sim/* | python lib/convert_to_sql.py network soc_snet_tt > tmp/network_snet_tt.sql
echo network creation...Done!
psql -U $db_username -d $crawl_name -f tmp/network_snet_tt.sql  

echo seeker selection
psql -U $db_username -d $crawl_name -f sqls/seeker.sql

echo Done!
