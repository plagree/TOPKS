
#!/usr/bin/env python

# Script to generate the test input file

def datafixed(f):
    with open(f, 'r') as f:
        maximum = 0
        for line in f:
            line = line.rstrip('\n')
            res = line.split('\t')
            if len(res) != 9:
                print 'ok'
                continue
            if maximum<int(res[5]):
                maximum = int(res[5])
    print maximum
    with open(f.rstrip('.txt')+'-fixed'+'.txt', 'w') as newFile:
        with open(f, 'r') as f:
            currU = -1
            currI = -1
            currTag = -1
            currFreq = -1
            currTime = -1
            currL = -1
            currAlpha = -1
            currThres = -1
            currRanking = -1
            for line in f:
                line = line.rstrip('\n')
                res = line.split('\t')
                if len(res) != 9:
                    print 'ok2'
                    continue
                if ((currU!=res[0]) or currTime!=res[4]) and (currU!=-1):
                    for i in range(int(currL)+1, maximum+1):
                        newFile.write(currU+'\t'+currI+'\t'+currTag+'\t'+currFreq+'\t'+currTime+'\t'+str(i)+'\t'+currAlpha+'\t'+currThres+'\t'+currRanking+'\n')
                if len(res)!=9:
                    print "ok"
                    continue
                currU = res[0]
                currI = res[1]
                currTag = res[2]
                currFreq = res[3]
                currTime = res[4]
                currL = res[5]
                currAlpha = res[6]
                currThres = res[7]
                currRanking = res[8]
                newFile.write(currU+'\t'+currI+'\t'+currTag+'\t'+currFreq+'\t'+currTime+'\t'+currL+'\t'+currAlpha+'\t'+currThres+'\t'+currRanking+'\n')
