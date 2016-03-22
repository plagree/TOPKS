#!/usr/bin/env python
#-*-coding: utf-8 -*-

import fileinput
from random import shuffle

PREFIX_LENGTH = 5
TAG_MIN_POPULARITY = 5

QUERY_POPULARITY_THRESHOLD = 10 # number under which queries are not popular and after popular
USER_ACTIVIVE_THRESHOLD = 100   # number under which a user is active or not
N_QUERIES = 3 # Number of queries to create

queries = dict()
users = dict()
for line in fileinput.input():
    user, item, tag  = line.rstrip().split('\t')
    if user not in users:
        users[user] = set()
    users[user].add(item)
    if len(tag) >= PREFIX_LENGTH:
        if tag not in queries:
            queries[tag] = 0
        queries[tag] += 1

# POPULARITY OF QUERIES
queries_popularity = list()
for k in queries:
    if queries[k] >= TAG_MIN_POPULARITY:
        queries_popularity.append((k, queries[k]))

# filter() : TODO filter popular vs non popular queries
good_queries = list()
for tup in queries_popularity:
    for i in range(tup[1]):
        good_queries.append(tup[0])
shuffle(good_queries)

# FILTER POPULAR OR UNPOPULAR USERS
good_users = list()
#users = filter(lambda x: len(x[1]) >= USER_ACTIVE_THRESHOLD, users)
for user in users:
    good_users.append(user)
shuffle(good_users)

for user, query in zip(good_users[:N_QUERIES], good_queries[:N_QUERIES]):
    print "%s\t%s" % (user, query)
