#!/usr/bin/env python

import sys
from operator import itemgetter

# Script to generate the test input file

#N = 2 # Number of connections minimum for user

if __name__=='__main__':
    if len(sys.argv) != 6:
        print "python script.py inputfile networkFile outputfile nItems nUsers"
    inputfile = sys.argv[1]
    networkFile = sys.argv[2]
    outputfile = sys.argv[3]
    N_ITEMS_OF_USER_U = int(sys.argv[4])
    N_USERS_OF_ITEM_I = int(sys.argv[5])

    # We create a dictionary with connected users (more than N edges)
    connectedUsers = {}
    with open(networkFile, 'r') as nf:
        for line in nf:
            line = line.rstrip('\n')
            u1, u2, _ = line.split('\t')
            if u1 not in connectedUsers:
                connectedUsers[u1] = True
            if u2 not in connectedUsers:
                connectedUsers[u2] = True
    print str(len(connectedUsers)), ' users connected'

    with open(outputfile, 'w') as newFile:
        tags = {}
        items = set()
        users = {}
        thresholdTags = 2
        goodTags, goodItems, goodUsers = dict(), dict(), dict()
        nbUserThisTagThisItem = dict()
        popularityTag = dict()
        with open(inputfile, 'r') as f:
            for line in f:
                line = line.rstrip('\n')
                res = line.split('\t')
                if res[2] not in tags:
                    tags[res[2]] = 0
                    popularityTag[res[2]] = set()
                tags[res[2]] += 1
                items.add(res[1])
                popularityTag[res[2]].add(res[1])
        for tag in tags.keys():
            if (tags[tag] >= thresholdTags) and (len(tag) > 2) and len(popularityTag[tag]) < (len(items)/2):
                goodTags[tag] = True

        print str(len(goodTags)), ' good tags'

        users = {}
        items = set()
        with open(inputfile, 'r') as f:
            for line in f:
                line = line.rstrip('\n')
                user, item, tag = line.split('\t')
                items.add(item)
                #if tag in goodTags:
                if True:
                    if item not in goodItems:
                        goodItems[item] = set()
                    goodItems[item].add(user)
        goodItemsCopy = {}
        for k in goodItems:
            if N_USERS_OF_ITEM_I > 0:
            	if len(goodItems[k]) >= N_USERS_OF_ITEM_I:
                    goodItemsCopy[k] = True
            else:
                if (len(goodItems[k]) <= abs(N_USERS_OF_ITEM_I)) and (len(goodItems[k]) > 1):
                    goodItemsCopy[k] = True
        goodItems = goodItemsCopy

        print str(len(goodItems)), ' good items'

        # HERE WE FILTER USERS WHO DO NOT HAVE AT LEAST n_items_of_user_u ITEMS
        item_tags = {} # list of tags per item by decreasing frequency
        with open(inputfile, 'r') as f:
            for line in f:
                line = line.rstrip('\n')
                user, item, tag = line.split('\t')
                if item not in item_tags:
                    item_tags[item] = {}
                if tag not in item_tags[item] :
                    item_tags[item][tag] = 0
                item_tags[item][tag] += 1
                if user not in users:
                    users[user] = set()
                users[user].add(item)
                if not (item in goodItems):
                    continue
                if nbUserThisTagThisItem.get(tag) is None:
                    nbUserThisTagThisItem[tag] = {}
                if nbUserThisTagThisItem[tag].get(item) is None:
                    nbUserThisTagThisItem[tag][item] = set()
                nbUserThisTagThisItem[tag][item].add(user)
        for i in item_tags:
            item_tags[i] = sorted(item_tags[i].items(), key=itemgetter(1), reverse=True)
            item_tags[i] = [A[0] for A in item_tags[i]]
        for user in users:
            if N_ITEMS_OF_USER_U > 0:
                if ((len(users[user]) >= N_ITEMS_OF_USER_U) and (user in connectedUsers)):
                    goodUsers[user] = True
            else:
                if ((len(users[user]) <= abs(N_ITEMS_OF_USER_U)) and (user in connectedUsers)):
                    goodUsers[user] = True

        print str(len(goodUsers)), ' good users'

        with open(inputfile, 'r') as f:
            counter = 0
            cc = 0
            for line in f:
                counter += 1
                line = line.rstrip('\n')
                user, item, tag  = line.split('\t')
                if len(tag) < 6:
                    continue
                if (user in goodUsers) and (tag in goodTags) and (item in goodItems):
                    if (len(nbUserThisTagThisItem[tag][item])>1):
                        newFile.write(user+'\t'+item+'\t'+tag+'\t'+str(len(nbUserThisTagThisItem[tag][item]))+'\n')
                        cc += 1
                        tags = [tag]
                        for ctag in item_tags[item]:
                            if (ctag==tag) or (len(ctag) < 6) or (len(tags)>2):
                                continue
                            isPrefix = False
                            for t in tags:
                                if ctag.startswith(t):
                                    isPrefix = True
                            if isPrefix:
                                continue
                            tags.append(ctag)
                        newFile.write(user+'\t'+item+'\t'+','.join(tags)+'\t'+','.join([str(len(nbUserThisTagThisItem[t][item])) for t in tags])+'\n')
                if cc > 100000:
                    break
