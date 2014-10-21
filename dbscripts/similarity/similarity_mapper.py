#!/usr/bin/env python

import sys
import itertools

if __name__=='__main__':
    for line in sys.stdin:
        taggers = set()
        line = line.strip().split('\t')
        tag = line[0]
        for usr in line[1].split(','): taggers.add(usr)
        for pair in itertools.combinations(taggers,2):
            print '%s#%s\t1' % (pair[0],pair[1])
