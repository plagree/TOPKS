#!/bin/bash
hadoop dfs -mkdir sim
hadoop dfs -rmr sim/dt_lst
hadoop dfs -rmr sim/dt_num
hadoop dfs -rmr sim/dt_sim

hadoop jar $HADOOP_HOME/contrib/streaming/hadoop-streaming-*.jar -D mapred.reduce.tasks=40\
    -D mapred.job.name='sim_dt:num'\
    -input twitter/triples/* -output sim/dt_num\
    -mapper 'python dt_num_mapper.py' -reducer 'python num_reducer.py'\
    -file dt_num_mapper.py -file num_reducer.py

hadoop jar $HADOOP_HOME/contrib/streaming/hadoop-streaming-*.jar -D mapred.reduce.tasks=40\
    -D mapred.job.name='sim_dt:lst'\
    -input sim/dt_num -output sim/dt_lst\
    -mapper /bin/cat -reducer 'python list_reducer.py'\
    -file list_reducer.py

hadoop jar $HADOOP_HOME/contrib/streaming/hadoop-streaming-*.jar -D mapred.reduce.tasks=40\
    -D mapred.job.name='sim_dt:sim'\
    -input sim/dt_lst -output sim/dt_sim\
    -mapper 'python similarity_mapper.py' -reducer 'python similarity_reducer.py'\
    -file similarity_mapper.py -file similarity_reducer.py
