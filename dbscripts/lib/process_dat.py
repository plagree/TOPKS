#!/usr/bin/env python

import sys

for line in sys.stdin:
    line = line.strip().split('\t')
    print '%s\t%s\t%s' %(line[0],line[1],line[2])
