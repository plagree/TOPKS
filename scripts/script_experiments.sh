#Script for experimentation

pathInputs=/home/lagree/datasets/twitter/
nDocs=1570866  #number of different items in the dataset
nLines=400

file1=test-file-tumblr-1.txt
file2=test-file-tumblr-2.txt

output1=output1-tumblr.txt
output2=output2-tumblr.txt

theta1=
theta2=
theta3=

network=user-to-user.txt

python script-input-experiment#1.py
cat test-file-1.txt | sort --random-sort | head -n 100000 > $file1
python script-input-experiment#2.py
cat test-file-2.txt | sort --random-sort | head -n 100000 > $file2

# java -jar -Xmx10000m executable.jar /path/to/files.txt numberOfDocuments networkFile inputTestFile outputFileName numberLinesTest

java -jar -Xmx12000m experiment_framework#final.jar $pathInputs $nDocs $network $file1 $output1 $nLines $theta1 $theta1 
java -jar -Xmx12000m experiment_framework#final.jar $pathInputs $nDocs $network $file1 $output1 $nLines $theta2 $theta1 
java -jar -Xmx12000m experiment_framework#final.jar $pathInputs $nDocs $network $file1 $output1 $nLines $theta3 $theta1 
java -jar -Xmx12000m experiment_framework#final.jar $pathInputs $nDocs $network $file2 $output2 $nLines $theta1 $theta1 
java -jar -Xmx12000m experiment_framework#final.jar $pathInputs $nDocs $network $file2 $output2 $nLines $theta2 $theta1 
java -jar -Xmx12000m experiment_framework#final.jar $pathInputs $nDocs $network $file2 $output2 $nLines $theta3 $theta1 
