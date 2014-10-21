#!/usr/bin/env python

import sys

def out(usr,keys):
    n = len(keys)
    for key in keys:
        print '%s\t%s:%d' % (key,usr,n)

if __name__=='__main__':
    curr_usr = ''
    keys = set()
    for line in sys.stdin:
        line = line.strip().split('\t')
        usr = line[0]
        key = line[1]
        if usr!=curr_usr:
            if curr_usr!='': out(curr_usr,keys)
            curr_usr = usr
            keys = set()
        keys.add(key)
    if curr_usr!='': out(curr_usr,keys)
