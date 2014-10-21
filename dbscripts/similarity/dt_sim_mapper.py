#!/usr/bin/env python

import sys

if __name__=='__main__':
    for line in sys.stdin:
        line = line.strip().split('\t')
        key = line[2]+'+'+line[3]
        usr = line[1]
        print '%s\t%s' % (key,usr)
