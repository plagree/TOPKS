#!/usr/bin/env python
#-*-coding: utf-8 -*-

import sys

sets = {"chinese": set(), "restaurant": set()}
for l in sys.stdin:
    u, i, t = l.rstrip().split()
    if t in sets:
        i = int(i)
        sets[t].add(i)
print sets["restaurant"].intersection(sets["chinese"])
