#!/usr/bin/env python
#-*-coding: utf-8 -*-

import sys
from operator import itemgetter

# Script to generate the test input file

if __name__=='__main__':
    if len(sys.argv) != 2:
        print "python script.py inputfile"
    inputfile = sys.argv[1]
    tags = {}
    with open(inputfile, 'r') as f:
        for line in f:
            line = line.rstrip('\n')
            res = line.split('\t')
            user = res[0]
            item = res[1]
            tag = res[2]
            if tag not in tags:
                tags[tag] = {}
            if item not in tags[tag]:
                tags[tag][item] = 0
            tags[tag][item] += 1
    with open('tag-inverted.txt', 'w') as f2:
        for tag in tags:
            il = sorted(tags[tag].items(), key=itemgetter(1), reverse=True)
            string = tag
            for tup in il:
                string += '\t' + tup[0] + ':' + str(tup[1])
            f2.write(string+'\n')
    tags = {}
    with open(inputfile, 'r') as f:
        for line in f:
            line = line.rstrip('\n')
            res = line.split('\t')
            user = res[0]
            item = res[1]
            tag = res[2]
            if tag not in tags:
                tags[tag] = set()
            tags[tag].add(item)
    with open('tag-freq.txt', 'w') as f2:
        for tag in tags:
            f2.write(tag+'\t'+str(len(tags[tag]))+'\n')
