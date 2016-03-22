#!/usr/bin/env python
#-*-coding: utf-8 -*-

import sys

s = set()
d = {}
#with open(sys.stdin, 'r') as f:
for l in sys.stdin:
    u, i, t = l.rstrip().split()
    i = int(i)
    if i == 35397:
        if t not in d:
            d[t] = set()
        d[t].add(int(u))
print d['restaurant']
print d['restaurants']
