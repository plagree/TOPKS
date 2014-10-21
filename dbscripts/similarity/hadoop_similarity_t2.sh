#!/bin/bash
hdfs dfs -mkdir sim
hdfs dfs -rm -r sim/t_lst
hdfs dfs -rm -r sim/t_num
hdfs dfs -rm -r sim/t_sim

hadoop jar $HADOOP_HOME/share/hadoop/tools/lib/hadoop-streaming-*.jar -D mapred.reduce.tasks=3\
    -D mapred.job.name='sim_t:num'\
    -input tagging -output sim/t_num\
    -mapper 'python t_num_mapper.py' -reducer 'python num_reducer.py'\
    -file t_num_mapper.py -file num_reducer.py

hadoop jar $HADOOP_HOME/share/hadoop/tools/lib/hadoop-streaming-*.jar -D mapred.reduce.tasks=3\
    -D mapred.job.name='sim_t:lst'\
    -input sim/t_num -output sim/t_lst\
    -mapper /bin/cat -reducer 'python list_reducer.py'\
    -file list_reducer.py

hadoop jar $HADOOP_HOME/share/hadoop/tools/lib/hadoop-streaming-*.jar -D mapred.reduce.tasks=3\
    -D mapred.skip.mode.enabled=true -D mapred.skip.map.max.skip.records=1\
    -D mapred.max.map.failures.percent=100\
    -D mapred.job.name='sim_t:sim'\
    -input sim/t_lst -output sim/t_sim\
    -mapper 'python similarity_mapper.py' -reducer 'python similarity_reducer.py'\
    -file similarity_mapper.py -file similarity_reducer.py

