#!/usr/bin/env python

import sys

def out(key,taggers):
    lst = ''
    for usr in taggers:
        lst += usr + ','
    print '%s\t%s' % (key,lst[:-1])

if __name__=='__main__':
    curr_key = ''
    taggers = set()
    for line in sys.stdin:
        line = line.strip().split('\t')
        key = line[0]
        usr = line[1]
        if key!=curr_key:
            if curr_key!='': out(curr_key,taggers)
            curr_key = key
            taggers = set()
        taggers.add(usr)
    if curr_key!='': out(curr_key,taggers)
