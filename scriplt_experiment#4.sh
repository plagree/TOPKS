#Script for experimentation

pathInputs=/home/lagree/datasets/twitter/
nDocs=1570866  #number of different items in the dataset
nLines=250

# java -jar -Xmx10000m executable.jar /path/to/files.txt numberOfDocuments networkFile inputTestFile outputFileName numberLinesTest

java -jar -Xmx12000m experiment_framework#4.jar $pathInputs $nDocs user-to-user.txt nameInput1.txt nameOutput1.txt $nLines 0.0
java -jar -Xmx12000m experiment_framework#4.jar $pathInputs $nDocs user-to-user.txt nameInput1.txt nameOutput2.txt $nLines 0.5
java -jar -Xmx12000m experiment_framework#4.jar $pathInputs $nDocs user-to-user.txt nameInput1.txt nameOutput3.txt $nLines 0.7
java -jar -Xmx12000m experiment_framework#4.jar $pathInputs $nDocs user-to-user.txt nameInput1.txt nameOutput4.txt $nLines 0.9
java -jar -Xmx12000m experiment_framework#4.jar $pathInputs $nDocs user-to-user.txt nameInput2.txt nameOutput5.txt $nLines 0.0
java -jar -Xmx12000m experiment_framework#4.jar $pathInputs $nDocs user-to-user.txt nameInput2.txt nameOutput6.txt $nLines 0.5
java -jar -Xmx12000m experiment_framework#4.jar $pathInputs $nDocs user-to-user.txt nameInput2.txt nameOutput7.txt $nLines 0.7
java -jar -Xmx12000m experiment_framework#4.jar $pathInputs $nDocs user-to-user.txt nameInput2.txt nameOutput8.txt $nLines 0.9
