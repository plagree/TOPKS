#!/usr/bin/env python

import sys

if __name__=='__main__':
    for line in sys.stdin:
        line = line.strip().split(',')
        key = '%s%s' % (line[1],line[2])
        usr = line[0]
        print '%s\t%s' % (usr,key)
