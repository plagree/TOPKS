#!/usr/bin/env python
#-*-coding: utf-8 -*-

import urllib2
import json
import numpy as np
import sys
import cPickle as pickle

TITLE = r'\textbf{Yelp social network}'
N = 100     # Number of queries for averaging
DOMAIN = 'http://localhost:8000/topks'
SUPERNODES = False

MAPPING = dict()
T = 2000
NEW_QUERY = True
N_NEIGH = 40000
THRESHOLD = 0.

def read_test_input(line):
    seeker, item, query = line.rstrip().split()
    return (int(seeker), int(item), query)

def NDCGrequest(seeker, tag, k=20, alpha=0):
    q = tag
    url = DOMAIN + \
            '?q=' + q + \
            '&seeker=' + str(seeker) + \
            '&alpha=' + str(alpha) + \
            '&k=' + str(k) + \
            '&t='+ str(T) + \
            '&newQuery=' + str(NEW_QUERY) + \
            '&nNeigh=' + str(N_NEIGH)
    result = None
    try:
        response = urllib2.urlopen(url)
        data = json.load(response)
        if not data.has_key('status'):
            return None
        if data.get('status') != 1:
            return None
        results = data['results']
        for res in results:
            if res['id'] == 35397:
                result = res['rank']
    except urllib2.HTTPError, error:
        print error.read()
    except (ValueError, KeyError, TypeError) as error:
        print error
    return result

def main(input_tests):
    results = dict()
    PREF_MIN = 2
    PREF_MAX = 6
    ALPHA = 0.
    with open('output.txt', 'a') as f2:
        with open(input_tests, 'r') as f:
            i = 0
            for line in f:
                if i >= N:
                    break
                seeker, query, tag = read_test_input(line)
                #if len(tag) < 6:
                #    continue
                i += 1
                print i
                for l in range(PREF_MIN, PREF_MAX+1):
                    result = NDCGrequest(seeker, tag[:l], k=1000, alpha=ALPHA)
                    f2.write('%d\t%d\t%s\t%d\t%d\t%d\t%f\t%f\t%d\t%d\t%d\t%d\n'
                             % (seeker, query, tag, -1, T, l, ALPHA,
                                THRESHOLD, result, 1, 3, 10))

if __name__ == '__main__':
    input_tests = sys.argv[1]
    main(input_tests)
