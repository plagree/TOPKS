#!/usr/bin/env python
#-*-coding: utf-8 -*-

s = set()
with open('triples.txt', 'r') as f:
    for l in f:
        u, i, t = l.rstrip().split('\t')
        if int(i) == 35397 and t[:6] == 'restau':
            print l,
        elif int(i) == 35397 and t == 'marble':
            print l,
        if t == "marble":
            s.add((int(i),t))

print s
