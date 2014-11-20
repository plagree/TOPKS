#!/usr/bin/env python

# Script to generate the test input file

with open('test-file-1.txt', 'w') as newFile:
    tags = {}
    items = set()
    users = {}
    thresholdTags = 200
    goodTags = {}
    goodItems = {}
    goodUsers = {}
    badItems = {}
    nbUserThisTagThisItem = {}
    with open('triples.txt', 'r') as f:
        for line in f:
            line = line.rstrip('\n')
            res = line.split('\t')
            if len(res) != 3:
                continue
            if tags.get(res[2]) is None:
                tags[res[2]] = 1
            else:
                tags[res[2]] = tags.get(res[2]) + 1
        for tag in tags.keys():
            if (tags[tag] >= thresholdTags) and (len(tag) > 2):
                goodTags[tag] = 1
    users = {}
    print str(len(goodTags)), " good tags"
    with open('triples.txt', 'r') as f:
        for line in f:
            line = line.rstrip('\n')
            res = line.split('\t')
            if len(res) != 3:
                continue
            user = res[0]
            item = res[1]
            tag = res[2]
            items.add(item)
            if not (tag in goodTags):
                badItems[item] = 1
    for item in list(items):
        if not (item in badItems):
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
            if not (item in goodItems):
                continue
            if nbUserThisTagThisItem.get(tag) is None:
                nbUserThisTagThisItem[tag] = {}
            if nbUserThisTagThisItem[tag].get(item) is None:
                nbUserThisTagThisItem[tag][item] = set()
            nbUserThisTagThisItem[tag][item].add(user)
            if users.get(user) is None:
                users[user] = set()
            users[user].add(item)
    for user in users:
        if (len(users[user]) >= 5):
            goodUsers[user] = 1
    print str(len(goodUsers)), " good users"
    with open('triples.txt', 'r') as f:
        for line in f:
            line = line.rstrip('\n')
            res = line.split('\t')
            if len(res) != 3:
                continue
            user = res[0]
            item = res[1]
            tag = res[2]
            if (user in goodUsers) and (tag in goodTags) and (item in goodItems):
                if (len(nbUserThisTagThisItem[tag][item])>1):
                    newFile.write(user+'\t'+item+'\t'+tag+'\t'+str(len(nbUserThisTagThisItem[tag][item]))+'\n')
