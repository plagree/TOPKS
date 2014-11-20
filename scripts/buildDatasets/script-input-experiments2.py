#!/usr/bin/env python

# Script to generate the test input file

with open('test-file-2.txt', 'w') as newFile:
    items = {}
    goodItems = {}
    nbUserThisTagThisItem = {}
    with open('triples.txt', 'r') as f:
        for line in f:
            line = line.rstrip('\n')
            res = line.split('\t')
            if len(res) != 3:
                continue
            user = res[0]
            item = res[1]
            tag = res[2]
            if items.get(item) is None:
                items[item] = set()
            items[item].add(user)
    for item in items:
        if (len(items[item]) >= 5) and (len(items[item]) <= 10):
            goodItems[item] = 1
    print str(len(goodItems)), " good items"
    with open('triples.txt', 'r') as f:
        for line in f:
            line = line.rstrip('\n')
            res = line.split('\t')
            if len(res) != 3:
                continue
            user = res[0]
            item = res[1]
            tag = res[2]
            if (item not in goodItems):
                continue
            if nbUserThisTagThisItem.get(tag) is None:
                nbUserThisTagThisItem[tag] = {}
            if nbUserThisTagThisItem[tag].get(item) is None:
                nbUserThisTagThisItem[tag][item] = set()
            nbUserThisTagThisItem[tag][item].add(user)
    with open('triples.txt', 'r') as f:
        for line in f:
            line = line.rstrip('\n')
            res = line.split('\t')
            if len(res) != 3:
                continue
            user = res[0]
            item = res[1]
            tag = res[2]
            if not (item in goodItems):
                continue
            if (item in goodItems) and (len(tag) > 2):
                if (len(nbUserThisTagThisItem[tag][item])>1):
                    newFile.write(user+'\t'+item+'\t'+tag+'\t'+str(len(nbUserThisTagThisItem[tag][item]))+'\n')
