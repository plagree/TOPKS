#!/usr/bin/env python

import sys

if __name__=='__main__':
   
    for line in sys.stdin:
        line = line.strip().split('\t')
        line[1] = line[1].replace('"','_')
        line[1] = line[1].replace('\'','_')
        line[2] = line[2].replace('"','_')
        line[2] = line[2].replace('\'','_')
 
        if sys.argv[1]=='tagging':
            print 'insert into tagging values(%d,\'%s\',\'%s\');' % (int(line[0]),line[1],line[2])
        elif sys.argv[1]=='soc_tag_80':
            print 'insert into soc_tag_80 values(%d,\'%s\',\'%s\');' % (int(line[0]),line[1],line[2])
        else:
            print 'insert into ', str(sys.argv[2]), ' values (%d, %d, %.10f );' % (int(line[0]), int(line[1]), float(line[2]))
