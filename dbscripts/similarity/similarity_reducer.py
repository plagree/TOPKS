#!/usr/bin/env python

import sys

def out(key,count):
    usr = key.split('#')
    usr1 = usr[0].split(':')
    usr2 = usr[1].split(':')
    sim = 2*float(count)/(float(usr1[1])+float(usr2[1]))
    print '%s\t%s\t%.10f' % (usr1[0],usr2[0],sim)
    print '%s\t%s\t%.10f' % (usr2[0],usr1[0],sim)
    #print 'insert into network values(%s,%s,%.10f);' % (usr1[0],usr2[0],sim)
    #print 'insert into network values(%s,%s,%.10f);' % (usr2[0],usr1[0],sim)

if __name__=='__main__':
    curr_key = ''
    count = 0
    for line in sys.stdin:
        line = line.strip().split('\t')
        key = line[0]
        cnt = int(line[1])
        if key!=curr_key:
            if curr_key!='': out(curr_key,count)
            curr_key = key
            count = 0
        count += cnt
    if curr_key!='': out(curr_key,count)
