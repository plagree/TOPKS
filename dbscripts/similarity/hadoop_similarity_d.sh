#!/bin/bash
hadoop dfs -mkdir sim
hadoop dfs -rmr sim/d_lst
hadoop dfs -rmr sim/d_num
hadoop dfs -rmr sim/d_sim

hadoop jar $HADOOP_HOME/contrib/streaming/hadoop-streaming-*.jar -D mapred.reduce.tasks=40\
    -D mapred.job.name='sim_d:num'\
    -input twitter/triples/* -output sim/d_num\
    -mapper 'python d_num_mapper.py' -reducer 'python num_reducer.py'\
    -file d_num_mapper.py -file num_reducer.py

hadoop jar $HADOOP_HOME/contrib/streaming/hadoop-streaming-*.jar -D mapred.reduce.tasks=40\
    -D mapred.job.name='sim_d:lst'\
    -input sim/d_num -output sim/d_lst\
    -mapper /bin/cat -reducer 'python list_reducer.py'\
    -file list_reducer.py

hadoop jar $HADOOP_HOME/contrib/streaming/hadoop-streaming-*.jar -D mapred.reduce.tasks=40\
    -D mapred.job.name='sim_d:sim'\
    -input sim/d_lst -output sim/d_sim\
    -mapper 'python similarity_mapper.py' -reducer 'python similarity_reducer.py'\
    -file similarity_mapper.py -file similarity_reducer.py
